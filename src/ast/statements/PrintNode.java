package ast.statements;

import java.util.List;
import ast.expressions.ExpressionNode;

/**
 * Sentencia que imprime una lista de expresiones.
 */
public class PrintNode extends StatementNode {
    private final List<ExpressionNode> arguments;

    public PrintNode(List<ExpressionNode> arguments) {
        this.arguments = arguments;
    }

    // Devuelve las expresiones que se imprimen.
    public List<ExpressionNode> getArguments() { return arguments; }
}
