package lexer;

/**
 * Excepcion lanzada cuando el lexer encuentra una secuencia invalida.
 */
public class LexicalException extends RuntimeException {
	private final int line;
	private final int column;

	public LexicalException(String message, int line, int column) {
		super("Error lexico en linea " + line + ", columna " + column + ": " + message);
		this.line = line;
		this.column = column;
	}

	// Devuelve la linea donde se detecto el error lexico.
	public int getLine() {
		return line;
	}

	// Devuelve la columna donde se detecto el error lexico.
	public int getColumn() {
		return column;
	}
}
