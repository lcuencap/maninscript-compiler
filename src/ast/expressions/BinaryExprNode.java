package ast.expressions;

/**
 * Expresion formada por dos operandos y un operador binario.
 */
public class BinaryExprNode extends ExpressionNode {
    private final ExpressionNode left;
    private final String operator;
    private final ExpressionNode right;

    public BinaryExprNode(ExpressionNode left, String operator, ExpressionNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    // Devuelve la expresion de la izquierda.
    public ExpressionNode getLeft() { return left; }

    // Devuelve el operador binario.
    public String getOperator() { return operator; }

    // Devuelve la expresion de la derecha.
    public ExpressionNode getRight() { return right; }
}
