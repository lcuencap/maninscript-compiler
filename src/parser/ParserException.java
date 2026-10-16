package parser;

import lexer.Token;

import java.util.Set;

/**
 * Excepcion lanzada cuando el parser encuentra una construccion invalida.
 */
public class ParserException extends RuntimeException {
    private static final Set<String> RESERVED_WORDS = Set.of(
            "unTal", "aMuerte", "fijateSi", "ySiNo", "segunVea", "seg\u00fanVea",
            "laFaena", "suelta", "di!", "naDeNa", "pavos", "aPachas",
            "carro", "letrilla", "jura", "jurao", "bulo", "y", "o"
    );

    private final int line;
    private final int column;
    private final String foundLexeme;
    private final String expected;

    public ParserException(Token token, String expected) {
        super(buildMessage(token, expected));
        this.line = token.getLine();
        this.column = token.getColumn();
        this.foundLexeme = token.getLexeme();
        this.expected = expected;
    }

    // Construye un mensaje de error sintactico siempre en espanol.
    private static String buildMessage(Token token, String expected) {
        if (expectsIdentifier(expected) && isReservedWord(token.getLexeme())) {
            return "Error sintactico en linea " + token.getLine()
                    + ", columna " + token.getColumn()
                    + ": se esperaba " + expected
                    + ", pero se encontro la palabra reservada '"
                    + token.getLexeme() + "'.";
        }

        return "Error sintactico en linea " + token.getLine()
                + ", columna " + token.getColumn()
                + ": se esperaba " + expected
                + ". Token encontrado: '" + token.getLexeme() + "'.";
    }

    private static boolean expectsIdentifier(String expected) {
        return expected != null && expected.contains("identificador");
    }

    private static boolean isReservedWord(String lexeme) {
        return lexeme != null && RESERVED_WORDS.contains(lexeme);
    }

    // Devuelve la linea del token donde fallo el parser.
    public int getLine() { return line; }

    // Devuelve la columna del token donde fallo el parser.
    public int getColumn() { return column; }

    // Devuelve el texto del token encontrado.
    public String getFoundLexeme() { return foundLexeme; }

    // Devuelve la descripcion del token o construccion esperada.
    public String getExpected() { return expected; }
}
