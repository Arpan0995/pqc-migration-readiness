package org.pqcreadiness.auditor.scan;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A project-wide table of {@code static final String} constants gathered from every
 * scanned source file, so cross-file references like
 * {@code KeyPairGenerator.getInstance(KeyUtils.RSA_ALGORITHM)} can be resolved to their
 * literal value — without a compiled classpath, because the auditor already has the
 * whole source tree in hand.
 *
 * <p>Only constants with a literal (or literal-concatenation) initialiser are indexed.
 * Qualified lookups ({@code Type.FIELD}) are always safe; bare lookups ({@code FIELD},
 * for static imports) are offered only when a field name maps to a single value across
 * the project, so an ambiguous name never resolves to an arbitrary one.
 */
final class ConstantIndex {

    /** An index with no entries; used when there is nothing to resolve against. */
    static final ConstantIndex EMPTY = new ConstantIndex(Map.of(), Map.of());

    private final Map<String, String> byQualifiedName;
    private final Map<String, String> byBareName;

    private ConstantIndex(Map<String, String> byQualifiedName, Map<String, String> byBareName) {
        this.byQualifiedName = byQualifiedName;
        this.byBareName = byBareName;
    }

    /** Look up {@code Type.FIELD}, e.g. {@code KeyUtils.RSA_ALGORITHM}. */
    Optional<String> lookupQualified(String typeName, String fieldName) {
        return Optional.ofNullable(byQualifiedName.get(typeName + "." + fieldName));
    }

    /** Look up a bare {@code FIELD} name; empty unless it is unambiguous project-wide. */
    Optional<String> lookupBare(String fieldName) {
        return Optional.ofNullable(byBareName.get(fieldName));
    }

    static ConstantIndex build(Collection<CompilationUnit> units) {
        Map<String, String> qualified = new HashMap<>();
        Map<String, Set<String>> bareValues = new HashMap<>();

        for (CompilationUnit unit : units) {
            for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) {
                String typeName = type.getNameAsString();
                for (FieldDeclaration field : type.findAll(FieldDeclaration.class)) {
                    if (!field.isStatic() || !field.isFinal()) {
                        continue;
                    }
                    for (VariableDeclarator variable : field.getVariables()) {
                        Optional<String> value = variable.getInitializer().flatMap(ConstantIndex::literal);
                        if (value.isEmpty()) {
                            continue;
                        }
                        qualified.put(typeName + "." + variable.getNameAsString(), value.get());
                        bareValues.computeIfAbsent(variable.getNameAsString(), k -> new HashSet<>())
                                .add(value.get());
                    }
                }
            }
        }

        Map<String, String> bare = new HashMap<>();
        bareValues.forEach((name, values) -> {
            if (values.size() == 1) {
                bare.put(name, values.iterator().next());
            }
        });
        return new ConstantIndex(Map.copyOf(qualified), Map.copyOf(bare));
    }

    /** Resolve a literal or literal-concatenation initializer to its string value. */
    private static Optional<String> literal(Expression expr) {
        if (expr instanceof StringLiteralExpr s) {
            return Optional.of(s.getValue());
        }
        if (expr instanceof BinaryExpr b && b.getOperator() == BinaryExpr.Operator.PLUS) {
            Optional<String> left = literal(b.getLeft());
            Optional<String> right = literal(b.getRight());
            if (left.isPresent() && right.isPresent()) {
                return Optional.of(left.get() + right.get());
            }
        }
        return Optional.empty();
    }
}
