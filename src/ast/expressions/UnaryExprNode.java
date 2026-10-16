package ast.expressions;

/**
 * Expresion con un operador unario y un operando.
 */
public class UnaryExprNode extends ExpressionNode {
    private final String operator;
    private final ExpressionNode right;

    public UnaryExprNode(String operator, ExpressionNode right) {
        this.operator = operator;
        this.right = right;
    }

    // Devuelve el operador unario.
    public String getOperator() { return operator; }

    // Devuelve la expresion a la que se aplica el operador.
    public ExpressionNode getRight() { return right; }
}
