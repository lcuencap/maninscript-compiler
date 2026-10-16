package lexer;

/**
 * Representa un token leido del codigo fuente.
 */
public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    // Devuelve el tipo del token.
    public TokenType getType() {
        return type;
    }

    // Devuelve el texto original del token.
    public String getLexeme() {
        return lexeme;
    }

    // Devuelve la linea donde empieza el token.
    public int getLine() {
        return line;
    }

    // Devuelve la columna donde empieza el token.
    public int getColumn() {
        return column;
    }

    // Representa el token en texto para depuracion.
    @Override
    public String toString() {
        return "Token{" +
                "tipo=" + type +
                ", lexema='" + lexeme + '\'' +
                ", linea=" + line +
                ", columna=" + column +
                '}';
    }
}
