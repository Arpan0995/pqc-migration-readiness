package org.pqcreadiness.auditor.scan;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

/**
 * Computes a stable key for the lexical scope enclosing an AST node.
 *
 * <p>Fragility indicators are attached to crypto findings that share an enclosing
 * scope (method/constructor, else type). This is a deliberately simple proxy for
 * dataflow adjacency: a fixed-size buffer in the same method as a signature call is
 * treated as co-located, without the fragility of full dataflow analysis.
 */
final class Scopes {

    private Scopes() {
    }

    static String keyFor(Node node) {
        return node.findAncestor(CallableDeclaration.class)
                .map(c -> "callable@" + position(c))
                .or(() -> node.findAncestor(TypeDeclaration.class)
                        .map(t -> "type@" + position(t)))
                .orElse("file");
    }

    private static String position(Node node) {
        return node.getRange().map(r -> r.begin.line + ":" + r.begin.column).orElse("?");
    }
}
