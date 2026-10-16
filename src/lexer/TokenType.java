package lexer;

/**
 * Enumera los tipos de token reconocidos por el lenguaje.
 */
public enum TokenType {
    UN_TAL, 
    A_MUERTE, 
    FIJATE_SI, 
    Y_SI_NO, 
    SEGUN_VEA, 
    LA_FAENA, 
    SUELTA, 
    DI,
    NA_DE_NA, 
    PAVOS, 
    APACHAS, 
    CARRO, 
    LETRILLA,
    JURA, 
    JURAO, 
    BULO,

    // Identificadores y literales.
    IDENTIFIER,
    INT_LITERAL,
    FLOAT_LITERAL,
    STRING_LITERAL,
    CHAR_LITERAL,


    // Operadores.
    ASSIGN,          // =
    EQUAL_EQUAL,     // ==
    LESS,            // <
    GREATER,         // >
    LESS_EQUAL,      // <=
    GREATER_EQUAL,   // >=
    AND,             // y
    OR,              // o
    PLUS,            // +
    MINUS,           // -
    STAR,            // *
    SLASH,           // /

    // Delimitadores.
    LBRACE,          // {
    RBRACE,          // }
    LPAREN,          // (
    RPAREN,          // )
    COMMA,           // ,
    SEMICOLON,       // ;

    EOF
}
