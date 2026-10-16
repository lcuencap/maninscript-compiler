package ast.expressions;

import java.util.List;

/**
 * Expresion que representa una llamada a funcion.
 */
public class CallExprNode extends ExpressionNode {
    private final String callee;
    private final List<ExpressionNode> arguments;

    public CallExprNode(String callee, List<ExpressionNode> arguments) {
        this.callee = callee;
        this.arguments = arguments;
    }

    // Devuelve el nombre de la funcion llamada.
    public String getCallee() { return callee; }

    // Devuelve los argumentos de la llamada.
    public List<ExpressionNode> getArguments() { return arguments; }
}
