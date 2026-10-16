package lexer;

import java.util.HashMap;
import java.util.Map;

/**
 * Analizador lexico que convierte el codigo fuente en tokens.
 */
public class Lexer {
	private static final Map<String, TokenType> KEYWORDS = buildKeywords();

	private final String source;

	private int start;
	private int current;
	private int line;
	private int column;

	private int tokenLine;
	private int tokenColumn;

	public Lexer(String source) {
		this.source = source == null ? "" : source;
		this.start = 0;
		this.current = 0;
		this.line = 1;
		this.column = 1;
		this.tokenLine = 1;
		this.tokenColumn = 1;
	}

	// Devuelve el siguiente token valido del codigo fuente.
	public Token readNextToken() {
		while (!isAtEnd()) {
			start = current;
			tokenLine = line;
			tokenColumn = column;

			Token token = readTokenOrSkip();
			if (token != null) {
				return token;
			}
		}

		return new Token(TokenType.EOF, "", line, column);
	}

	// Lee un token o descarta espacios y comentarios.
	private Token readTokenOrSkip() {
		char c = advance();
		switch (c) {
			case ' ':
			case '\r':
			case '\t':
			case '\n':
				return null;
			case '{':
				return createToken(TokenType.LBRACE);
			case '}':
				return createToken(TokenType.RBRACE);
			case '(':
				return createToken(TokenType.LPAREN);
			case ')':
				return createToken(TokenType.RPAREN);
			case ',':
				return createToken(TokenType.COMMA);
			case ';':
				return createToken(TokenType.SEMICOLON);
			case '+':
				return createToken(TokenType.PLUS);
			case '-':
				return createToken(TokenType.MINUS);
			case '*':
				return createToken(TokenType.STAR);
			case '=':
				return createToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.ASSIGN);
			case '<':
				return createToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
			case '>':
				return createToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
			case '/':
				if (match('/')) {
					skipLineComment();
					return null;
				} else if (match('*')) {
					skipBlockComment();
					return null;
				} else {
					return createToken(TokenType.SLASH);
				}
			case '"':
				return string();
			case '\'':
				return character();
			default:
				if (isDigit(c)) {
					return number();
				} else if (isIdentifierStart(c)) {
					return identifier();
				} else {
					throw error(unexpectedCharacterMessage(c));
				}
		}
	}

	// Salta todo el contenido de un comentario de linea.
	private void skipLineComment() {
		while (!isAtEnd() && peek() != '\n') {
			advance();
		}
	}

	// Salta un comentario de bloque y avisa si no se cierra.
	private void skipBlockComment() {
		while (!isAtEnd()) {
			if (peek() == '*' && peekNext() == '/') {
				advance();
				advance();
				return;
			}
			advance();
		}

		throw error("Comentario de bloque sin cerrar. Falta '*/' antes del final del fichero.");
	}

	// Lee una cadena entre comillas dobles.
	private Token string() {
		StringBuilder value = new StringBuilder();

		while (!isAtEnd()) {
			if (peek() == '"') {
				advance();
				return createToken(TokenType.STRING_LITERAL, value.toString());
			}

			if (peek() == '\n') {
				throw error("Cadena de texto sin cerrar. Falta '\"' de cierre antes del final de linea.");
			}

			if (peek() == '\\') {
				advance();
				if (isAtEnd()) {
					throw error("Secuencia de escape incompleta en cadena de texto. Despues de '\\' se esperaba "
							+ allowedEscapeDescription() + ".");
				}
				value.append(readEscapedChar(advance()));
				continue;
			}

			value.append(advance());
		}

		throw error("Cadena de texto sin cerrar. Falta '\"' de cierre antes del final del fichero.");
	}

	// Lee un literal de caracter entre comillas simples.
	private Token character() {
		if (isAtEnd() || peek() == '\n') {
			throw error("Literal de caracter sin cerrar. Falta la comilla simple de cierre.");
		}

		char value;
		if (peek() == '\\') {
			advance();
			if (isAtEnd()) {
				throw error("Secuencia de escape incompleta en literal de caracter. Despues de '\\' se esperaba "
						+ allowedEscapeDescription() + ".");
			}
			value = readEscapedChar(advance());
		} else {
			value = advance();
		}

		if (!match('\'')) {
			throw error("Literal de caracter invalido. Debe contener exactamente un caracter o una secuencia de "
					+ "escape valida entre comillas simples.");
		}

		return createToken(TokenType.CHAR_LITERAL, String.valueOf(value));
	}

	// Convierte las secuencias de escape permitidas a su caracter real.
	private char readEscapedChar(char escapeCode) {
		switch (escapeCode) {
			case 'n':
				return '\n';
			case 't':
				return '\t';
			case 'r':
				return '\r';
			case '\'':
				return '\'';
			case '"':
				return '"';
			case '\\':
				return '\\';
			default:
				throw error("Secuencia de escape no valida: \\" + escapeCode
						+ ". Escapes permitidos: " + allowedEscapeDescription() + ".");
		}
	}

	// Lee numeros enteros o decimales.
	private Token number() {
		while (isDigit(peek())) {
			advance();
		}

		boolean isFloat = false;
		if (peek() == '.' && isDigit(peekNext())) {
			isFloat = true;
			advance(); // Consume el punto decimal.
			while (isDigit(peek())) {
				advance();
			}
		}

		String lexeme = source.substring(start, current);
		return createToken(isFloat ? TokenType.FLOAT_LITERAL : TokenType.INT_LITERAL, lexeme);
	}

	// Lee identificadores y palabras reservadas.
	private Token identifier() {
		while (isIdentifierPart(peek())) {
			advance();
		}

		String lexeme = source.substring(start, current);

		// Soporta la forma "di!" sin crear token separado para '!'.
		if ("di".equals(lexeme) && match('!')) {
			return createToken(TokenType.DI, "di!");
		}

		TokenType keywordType = KEYWORDS.get(lexeme);
		if (keywordType != null) {
			return createToken(keywordType, lexeme);
		}

		return createToken(TokenType.IDENTIFIER, lexeme);
	}

	// Crea un token usando el texto leido del codigo fuente.
	private Token createToken(TokenType type) {
		return createToken(type, source.substring(start, current));
	}

	// Crea un token con un lexema especifico.
	private Token createToken(TokenType type, String lexeme) {
		return new Token(type, lexeme, tokenLine, tokenColumn);
	}

	// Avanza un caracter y actualiza linea y columna.
	private char advance() {
		char c = source.charAt(current);
		current++;

		if (c == '\n') {
			line++;
			column = 1;
		} else {
			column++;
		}

		return c;
	}

	// Consume el caracter esperado si esta en la posicion actual.
	private boolean match(char expected) {
		if (isAtEnd()) {
			return false;
		}

		if (source.charAt(current) != expected) {
			return false;
		}

		advance();
		return true;
	}

	// Mira el caracter actual sin consumirlo.
	private char peek() {
		if (isAtEnd()) {
			return '\0';
		}
		return source.charAt(current);
	}

	// Mira el siguiente caracter sin consumirlo.
	private char peekNext() {
		if (current + 1 >= source.length()) {
			return '\0';
		}
		return source.charAt(current + 1);
	}

	// Indica si ya se leyo todo el codigo fuente.
	private boolean isAtEnd() {
		return current >= source.length();
	}

	// Comprueba si un caracter es un digito decimal.
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	// Comprueba si un caracter puede empezar un identificador.
	private boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_';
	}

	// Comprueba si un caracter puede continuar un identificador.
	private boolean isIdentifierPart(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	// Crea un error lexico en la posicion del token actual.
	private LexicalException error(String message) {
		return new LexicalException(message, tokenLine, tokenColumn);
	}

	// Explica el caso probable tras un caracter no reconocido.
	private String unexpectedCharacterMessage(char c) {
		return switch (c) {
			case '!' -> "Caracter no reconocido: '!'. El lenguaje no admite '!' suelto ni '!=', solo la palabra reservada 'di!'.";
			case '&' -> "Caracter no reconocido: '&'. El AND logico se escribe con la palabra reservada 'y'.";
			case '|' -> "Caracter no reconocido: '|'. El OR logico se escribe con la palabra reservada 'o'.";
			case '.' -> "Caracter no reconocido: '.'. Si querias un literal decimal, el punto debe aparecer entre digitos, por ejemplo 3.14.";
			default -> "Caracter no reconocido: '" + c + "'.";
		};
	}

	// Lista los escapes validos para reutilizarlos en los mensajes de error.
	private String allowedEscapeDescription() {
		return "\\n, \\t, \\r, \\\\, \\\" o \\'";
	}

	// Construye la tabla de palabras reservadas del lenguaje.
	private static Map<String, TokenType> buildKeywords() {
		Map<String, TokenType> keywords = new HashMap<>();

		keywords.put("unTal", TokenType.UN_TAL);
		keywords.put("aMuerte", TokenType.A_MUERTE);
		keywords.put("fijateSi", TokenType.FIJATE_SI);
		keywords.put("ySiNo", TokenType.Y_SI_NO);
		keywords.put("segunVea", TokenType.SEGUN_VEA);
		keywords.put("seg\u00fanVea", TokenType.SEGUN_VEA);
		keywords.put("laFaena", TokenType.LA_FAENA);
		keywords.put("suelta", TokenType.SUELTA);
		keywords.put("naDeNa", TokenType.NA_DE_NA);
		keywords.put("pavos", TokenType.PAVOS);
		keywords.put("aPachas", TokenType.APACHAS);
		keywords.put("carro", TokenType.CARRO);
		keywords.put("letrilla", TokenType.LETRILLA);
		keywords.put("jura", TokenType.JURA);
		keywords.put("jurao", TokenType.JURAO);
		keywords.put("bulo", TokenType.BULO);

		keywords.put("y", TokenType.AND);
		keywords.put("o", TokenType.OR);

		return keywords;
	}
}
