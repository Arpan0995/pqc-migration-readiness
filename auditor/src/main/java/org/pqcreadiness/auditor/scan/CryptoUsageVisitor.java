package org.pqcreadiness.auditor.scan;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.pqcreadiness.auditor.model.Category;
import org.pqcreadiness.auditor.model.Confidence;
import org.pqcreadiness.auditor.model.Finding;
import org.pqcreadiness.auditor.rules.EntryPoint;
import org.pqcreadiness.auditor.rules.VulnerableAlgorithms;

import java.util.List;
import java.util.Optional;

/**
 * Detection visitor covering the JCA factory entry points (wave 1 &amp; 2) and
 * concrete key-type coupling (fragility indicator F4).
 *
 * <p>Detection is syntactic: an entry point is recognised by the receiver's trailing
 * type name and the algorithm must be a string literal. This resolves without a
 * classpath, which is what lets the auditor run on arbitrary case-study codebases
 * that we do not build. Constant propagation and full symbol resolution are possible
 * future refinements but are intentionally not required.
 */
final class CryptoUsageVisitor extends VoidVisitorAdapter<ScanContext> {

    private static final String GET_INSTANCE = "getInstance";

    @Override
    public void visit(MethodCallExpr call, ScanContext ctx) {
        super.visit(call, ctx);
        if (!GET_INSTANCE.equals(call.getNameAsString()) || call.getArguments().isEmpty()) {
            return;
        }
        Optional<EntryPoint> entryPoint = call.getScope()
                .flatMap(CryptoUsageVisitor::trailingName)
                .flatMap(EntryPoint::forReceiver);
        if (entryPoint.isEmpty()) {
            return;
        }
        Optional<String> literal = stringLiteral(call.getArgument(0));
        if (literal.isEmpty()) {
            return;
        }
        EntryPoint ep = entryPoint.get();
        ep.match(literal.get()).ifPresent(family -> {
            int line = call.getRange().map(r -> r.begin.line).orElse(-1);
            int column = call.getRange().map(r -> r.begin.column).orElse(-1);
            ctx.add(new Finding(ep.ruleId(family), ctx.file(), line, column,
                    ep.receiverType(), family, ep.category(), Confidence.HIGH,
                    List.of(), call.toString()));
        });
    }

    /**
     * Concrete key interface types used in a type position (variable declarations,
     * fields, parameters) — fragility indicator F4, "concrete-type coupling". Coupling
     * a public surface to {@code RSAPublicKey}/{@code ECPrivateKey}/... propagates a
     * migration through every caller.
     */
    @Override
    public void visit(ClassOrInterfaceType type, ScanContext ctx) {
        super.visit(type, ctx);
        String name = type.getNameAsString();
        if (!VulnerableAlgorithms.CONCRETE_KEY_TYPES.contains(name)) {
            return;
        }
        int line = type.getRange().map(r -> r.begin.line).orElse(-1);
        int column = type.getRange().map(r -> r.begin.column).orElse(-1);
        ctx.add(new Finding("FRAG-F4-" + name, ctx.file(), line, column,
                "key-type", name, Category.TYPE_COUPLING, Confidence.MEDIUM,
                List.of("F4"), name));
    }

    /** The trailing identifier of a receiver expression: {@code javax.crypto.Cipher} -> {@code Cipher}. */
    private static Optional<String> trailingName(Expression scope) {
        if (scope instanceof NameExpr name) {
            return Optional.of(name.getNameAsString());
        }
        if (scope instanceof FieldAccessExpr field) {
            return Optional.of(field.getNameAsString());
        }
        return Optional.empty();
    }

    private static Optional<String> stringLiteral(Expression arg) {
        return arg instanceof StringLiteralExpr s ? Optional.of(s.getValue()) : Optional.empty();
    }
}
