package semantic;

import ast.ASTNode;

/**
 * Excepcion lanzada cuando se detecta un error semantico.
 */
public class SemanticException extends RuntimeException {
    private final int line;
    private final int column;

    public SemanticException(ASTNode node, String message) {
        super(buildMessage(node, message));
        this.line = node == null ? -1 : node.getLine();
        this.column = node == null ? -1 : node.getColumn();
    }

    // Construye un mensaje de error semantico siempre en espanol.
    private static String buildMessage(ASTNode node, String message) {
        if (node == null || node.getLine() < 1 || node.getColumn() < 1) {
            return "Error semantico: " + message;
        }

        return "Error semantico en linea " + node.getLine()
                + ", columna " + node.getColumn()
                + ": " + message;
    }

    // Devuelve la linea donde se detecto el error.
    public int getLine() {
        return line;
    }

    // Devuelve la columna donde se detecto el error.
    public int getColumn() {
        return column;
    }
}
