package org.pqcreadiness.auditor.scan;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.pqcreadiness.auditor.model.Confidence;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a method-argument expression to a constant string, following simple
 * intra-file constant propagation so that {@code getInstance(alg)} is caught when
 * {@code alg} is a local variable or a field initialised to a literal — not only the
 * direct {@code getInstance("RSA")} form.
 *
 * <p>Confidence reflects how the value was obtained: {@link Confidence#HIGH} for a pure
 * string literal (or literal concatenation), {@link Confidence#MEDIUM} when any variable
 * or field indirection was followed.
 *
 * <p>Scope is deliberately intra-file and syntactic, consistent with the rest of the
 * scanner: it does <em>not</em> resolve constants declared in other compilation units
 * (that needs a compiled classpath we do not require) nor algorithm choices expressed
 * through enums/registries (e.g. a library's {@code SignatureAlgorithm.RS256}), which
 * are library semantics rather than constant propagation.
 */
final class ConstantResolver {

    /** A resolved constant string and the confidence with which it was resolved. */
    record Resolved(String value, Confidence confidence) {
    }

    /** Guards against pathological reference chains; real constant chains are shallow. */
    private static final int MAX_DEPTH = 8;

    private ConstantResolver() {
    }

    static Optional<Resolved> resolve(Expression expr, ConstantIndex index) {
        return resolve(expr, index, MAX_DEPTH);
    }

    private static Optional<Resolved> resolve(Expression expr, ConstantIndex index, int depth) {
        if (depth <= 0 || expr == null) {
            return Optional.empty();
        }
        if (expr instanceof StringLiteralExpr s) {
            return Optional.of(new Resolved(s.getValue(), Confidence.HIGH));
        }
        if (expr instanceof EnclosedExpr enclosed) {
            return resolve(enclosed.getInner(), index, depth - 1);
        }
        if (expr instanceof CastExpr cast) {
            return resolve(cast.getExpression(), index, depth - 1);
        }
        if (expr instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            return resolveConcat(binary, index, depth);
        }
        if (expr instanceof NameExpr name) {
            return resolveIdentifier(expr, name.getNameAsString(), index, depth);
        }
        if (expr instanceof FieldAccessExpr field) {
            return resolveFieldAccess(field, index, depth);
        }
        return Optional.empty();
    }

    private static Optional<Resolved> resolveConcat(BinaryExpr binary, ConstantIndex index, int depth) {
        Optional<Resolved> left = resolve(binary.getLeft(), index, depth - 1);
        Optional<Resolved> right = resolve(binary.getRight(), index, depth - 1);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        Confidence confidence = left.get().confidence() == Confidence.HIGH
                && right.get().confidence() == Confidence.HIGH
                ? Confidence.HIGH : Confidence.MEDIUM;
        return Optional.of(new Resolved(left.get().value() + right.get().value(), confidence));
    }

    /**
     * A bare identifier: try an enclosing-scope local/field first, then a project-wide
     * bare constant (a static-imported name). Any value reached this way is MEDIUM.
     */
    private static Optional<Resolved> resolveIdentifier(Node from, String name,
                                                        ConstantIndex index, int depth) {
        return resolveLocalVariable(from, name, index, depth)
                .or(() -> resolveField(from, name, index, depth))
                .or(() -> index.lookupBare(name))
                .map(value -> new Resolved(value, Confidence.MEDIUM));
    }

    /**
     * A qualified {@code Type.FIELD} access: try the enclosing type's own field, then the
     * project-wide constant index keyed by the qualifier's simple type name.
     */
    private static Optional<Resolved> resolveFieldAccess(FieldAccessExpr field,
                                                         ConstantIndex index, int depth) {
        String fieldName = field.getNameAsString();
        Optional<String> local = resolveField(field, fieldName, index, depth);
        if (local.isPresent()) {
            return local.map(value -> new Resolved(value, Confidence.MEDIUM));
        }
        if (field.getScope() instanceof NameExpr qualifier) {
            return index.lookupQualified(qualifier.getNameAsString(), fieldName)
                    .map(value -> new Resolved(value, Confidence.MEDIUM));
        }
        return Optional.empty();
    }

    /** A local variable in the enclosing method, uniquely declared, initialised, not reassigned. */
    private static Optional<String> resolveLocalVariable(Node from, String name,
                                                         ConstantIndex index, int depth) {
        Optional<CallableDeclaration> method = from.findAncestor(CallableDeclaration.class);
        if (method.isEmpty()) {
            return Optional.empty();
        }
        CallableDeclaration<?> callable = method.get();
        boolean reassigned = callable.findAll(AssignExpr.class).stream()
                .anyMatch(a -> a.getTarget() instanceof NameExpr t && t.getNameAsString().equals(name));
        if (reassigned) {
            return Optional.empty();
        }
        List<VariableDeclarator> declarators = callable.findAll(VariableDeclarator.class).stream()
                .filter(v -> v.getNameAsString().equals(name) && v.getInitializer().isPresent())
                .toList();
        if (declarators.size() != 1) {
            return Optional.empty();
        }
        return resolve(declarators.get(0).getInitializer().orElseThrow(), index, depth - 1)
                .map(Resolved::value);
    }

    /** A {@code final} field of the enclosing type initialised to a resolvable constant. */
    private static Optional<String> resolveField(Node from, String name,
                                                 ConstantIndex index, int depth) {
        Optional<TypeDeclaration> type = from.findAncestor(TypeDeclaration.class);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        for (FieldDeclaration field : type.get().findAll(FieldDeclaration.class)) {
            if (!field.isFinal()) {
                continue;
            }
            for (VariableDeclarator variable : field.getVariables()) {
                if (variable.getNameAsString().equals(name) && variable.getInitializer().isPresent()) {
                    return resolve(variable.getInitializer().orElseThrow(), index, depth - 1)
                            .map(Resolved::value);
                }
            }
        }
        return Optional.empty();
    }
}
