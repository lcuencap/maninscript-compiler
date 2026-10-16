package ast.statements;

import ast.expressions.ExpressionNode;

/**
 * Sentencia de bucle con condicion previa.
 */
public class WhileNode extends StatementNode {
    private final ExpressionNode condition;
    private final BlockNode body;

    public WhileNode(ExpressionNode condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }

    // Devuelve la condicion del bucle.
    public ExpressionNode getCondition() { return condition; }

    // Devuelve el cuerpo del bucle.
    public BlockNode getBody() { return body; }
}
