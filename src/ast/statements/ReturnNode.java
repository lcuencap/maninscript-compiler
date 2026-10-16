package ast.statements;

import ast.expressions.ExpressionNode;

/**
 * Sentencia de retorno con valor opcional.
 */
public class ReturnNode extends StatementNode {
    private final ExpressionNode expression;

    public ReturnNode(ExpressionNode expression) {
        this.expression = expression;
    }

    // Devuelve la expresion devuelta, si existe.
    public ExpressionNode getExpression() { return expression; }
}
