package ast.statements;

import java.util.List;

/**
 * Sentencia compuesta que agrupa varias sentencias en bloque.
 */
public class BlockNode extends StatementNode {
    private final List<StatementNode> statements;

    public BlockNode(List<StatementNode> statements) {
        this.statements = statements;
    }

    // Devuelve las sentencias contenidas en el bloque.
    public List<StatementNode> getStatements() {
        return statements;
    }
}
