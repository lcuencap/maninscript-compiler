package ast.statements;
import ast.expressions.ExpressionNode;

/**
 * Sentencia que asigna una expresion a una variable.
 */
public class AssignmentNode extends StatementNode {
    private final String identifier;
    private final ExpressionNode expression;

    public AssignmentNode(String identifier, ExpressionNode expression) {
        this.identifier = identifier;
        this.expression = expression;
    }

    // Devuelve el identificador que recibe la asignacion.
    public String getIdentifier() { return identifier; }

    // Devuelve la expresion asignada.
    public ExpressionNode getExpression() { return expression; }
}
