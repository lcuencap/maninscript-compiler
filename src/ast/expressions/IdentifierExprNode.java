package ast.expressions;

/**
 * Expresion que referencia un identificador.
 */
public class IdentifierExprNode extends ExpressionNode {
    private final String name;

    public IdentifierExprNode(String name) {
        this.name = name;
    }

    // Devuelve el nombre del identificador.
    public String getName() { return name; }
}
