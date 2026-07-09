package org.pqcreadiness.auditor.scan;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.rules.EntryPoint;
import org.pqcreadiness.auditor.rules.FragilityRules;
import org.pqcreadiness.auditor.rules.VulnerableAlgorithms;

import java.util.List;
import java.util.Optional;

/**
 * Detection visitor covering the JCA factory entry points (waves 1&ndash;2), concrete
 * key-type coupling (F4), and the structural fragility signals F1 (fixed-size buffers),
 * F3 (protocol/suite pinning), and F6 (persisted key material).
 *
 * <p>Detection is syntactic: entry points are recognised by the receiver's trailing
 * type name and algorithms must be string literals, so a scan needs no classpath and
 * never aborts. Fragility signals are emitted with their enclosing-scope key; the
 * {@link Scanner} merges them onto co-located crypto findings.
 */
final class DetectionVisitor extends VoidVisitorAdapter<ScanContext> {

    private static final String GET_INSTANCE = "getInstance";
    private static final String KEY_STORE = "KeyStore";
    private static final String BYTE_BUFFER = "ByteBuffer";

    @Override
    public void visit(MethodCallExpr call, ScanContext ctx) {
        super.visit(call, ctx);
        String method = call.getNameAsString();
        Optional<String> receiver = call.getScope().flatMap(DetectionVisitor::trailingName);

        // F3: pinning the enabled cipher-suite / protocol list.
        if (FragilityRules.TLS_PINNING_METHODS.contains(method)) {
            String rule = method.equals("setEnabledProtocols") ? "TLS-PROTOCOLS-PINNED" : "TLS-SUITES-PINNED";
            add(ctx, call, rule, method, "pinned", Category.TLS_CONFIG, Confidence.HIGH, List.of("F3"));
            ctx.addSignal("F3", Scopes.keyFor(call));
            return;
        }

        if (!GET_INSTANCE.equals(method) || call.getArguments().isEmpty()) {
            // F1: ByteBuffer.allocate(sentinel).
            byteBufferAllocation(call, receiver, ctx);
            return;
        }
        // Resolve the algorithm argument, following intra- and cross-file constant propagation.
        Optional<ConstantResolver.Resolved> resolved =
                ConstantResolver.resolve(call.getArgument(0), ctx.constants());
        if (resolved.isEmpty() || receiver.isEmpty()) {
            return;
        }
        String algorithm = resolved.get().value();
        Confidence confidence = resolved.get().confidence();
        String recv = receiver.get();

        Optional<EntryPoint> entryPoint = EntryPoint.forReceiver(recv);
        if (entryPoint.isPresent()) {
            EntryPoint ep = entryPoint.get();
            ep.match(algorithm).ifPresent(family ->
                    add(ctx, call, ep.ruleId(family), ep.receiverType(), family,
                            ep.category(), confidence, List.of()));
            return;
        }
        // F3: SSLContext.getInstance("TLSv1.2") and similar legacy version pins.
        if (FragilityRules.TLS_CONTEXT_RECEIVERS.contains(recv)
                && FragilityRules.isPinnedTlsVersion(algorithm)) {
            String version = VulnerableAlgorithms.normalize(algorithm);
            add(ctx, call, "TLS-PIN-" + version, recv, version,
                    Category.TLS_CONFIG, confidence, List.of("F3"));
            ctx.addSignal("F3", Scopes.keyFor(call));
            return;
        }
        // F6: persisted key material via a keystore type.
        if (recv.equals(KEY_STORE) && FragilityRules.isKeystoreType(algorithm)) {
            ctx.addSignal("F6", Scopes.keyFor(call));
        }
    }

    private void byteBufferAllocation(MethodCallExpr call, Optional<String> receiver, ScanContext ctx) {
        if (receiver.filter(BYTE_BUFFER::equals).isPresent()
                && FragilityRules.BYTE_BUFFER_ALLOCATORS.contains(call.getNameAsString())
                && !call.getArguments().isEmpty()) {
            sentinelSize(call.getArgument(0))
                    .ifPresent(size -> ctx.addSignal("F1", Scopes.keyFor(call)));
        }
    }

    /** F1: {@code new byte[C]} with a sentinel classical size. */
    @Override
    public void visit(ArrayCreationExpr array, ScanContext ctx) {
        super.visit(array, ctx);
        boolean byteArray = array.getElementType().asString().equals("byte");
        if (!byteArray || array.getLevels().isEmpty()) {
            return;
        }
        array.getLevels().getFirst()
                .flatMap(level -> level.getDimension())
                .flatMap(DetectionVisitor::sentinelSize)
                .ifPresent(size -> ctx.addSignal("F1", Scopes.keyFor(array)));
    }

    /** F6: constructing an encoded key spec over serialized key material. */
    @Override
    public void visit(ObjectCreationExpr creation, ScanContext ctx) {
        super.visit(creation, ctx);
        if (FragilityRules.KEY_SPEC_TYPES.contains(creation.getType().getNameAsString())) {
            ctx.addSignal("F6", Scopes.keyFor(creation));
        }
    }

    /** F4: concrete key interface type used in a type position (field/param/return/local). */
    @Override
    public void visit(ClassOrInterfaceType type, ScanContext ctx) {
        super.visit(type, ctx);
        String name = type.getNameAsString();
        if (!VulnerableAlgorithms.CONCRETE_KEY_TYPES.contains(name)) {
            return;
        }
        add(ctx, type, "FRAG-F4-" + name, "key-type", name,
                Category.TYPE_COUPLING, Confidence.MEDIUM, List.of("F4"));
    }

    private static void add(ScanContext ctx, com.github.javaparser.ast.Node node, String ruleId,
                            String api, String algorithm, Category category,
                            Confidence confidence, List<String> fragility) {
        int line = node.getRange().map(r -> r.begin.line).orElse(-1);
        int column = node.getRange().map(r -> r.begin.column).orElse(-1);
        String snippet = node.toString();
        ctx.addFinding(new Finding(ruleId, ctx.file(), line, column, api, algorithm,
                category, confidence, fragility, snippet), Scopes.keyFor(node));
    }

    private static Optional<Integer> sentinelSize(Expression expr) {
        if (expr instanceof IntegerLiteralExpr lit) {
            int value = lit.asNumber().intValue();
            if (FragilityRules.SENTINEL_SIZES.contains(value)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> trailingName(Expression scope) {
        if (scope instanceof NameExpr name) {
            return Optional.of(name.getNameAsString());
        }
        if (scope instanceof FieldAccessExpr field) {
            return Optional.of(field.getNameAsString());
        }
        return Optional.empty();
    }
}
