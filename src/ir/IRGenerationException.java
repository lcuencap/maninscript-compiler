package ir;

import ast.ASTNode;

/**
 * Excepcion lanzada cuando falla la generacion de IR.
 */
public class IRGenerationException extends RuntimeException {
    private final int line;
    private final int column;

    public IRGenerationException(ASTNode node, String message) {
        super(buildMessage(node, message));
        this.line = node == null ? -1 : node.getLine();
        this.column = node == null ? -1 : node.getColumn();
    }

    // Devuelve la linea asociada al error, si existe.
    public int getLine() {
        return line;
    }

    // Devuelve la columna asociada al error, si existe.
    public int getColumn() {
        return column;
    }

    private static String buildMessage(ASTNode node, String message) {
        if (node == null || node.getLine() < 1 || node.getColumn() < 1) {
            return "Error generando IR: " + message;
        }

        return "Error generando IR en linea " + node.getLine()
                + ", columna " + node.getColumn()
                + ": " + message;
    }
}
