package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import ast.ASTNode;
import ast.GlobalElement;
import ast.ProgramNode;
import ast.expressions.BinaryExprNode;
import ast.expressions.CallExprNode;
import ast.expressions.ExpressionNode;
import ast.expressions.IdentifierExprNode;
import ast.expressions.LiteralExprNode;
import ast.expressions.UnaryExprNode;
import ast.statements.AssignmentNode;
import ast.statements.BlockNode;
import ast.statements.CallStatementNode;
import ast.statements.DeclarationNode;
import ast.statements.FunctionDeclNode;
import ast.statements.IfNode;
import ast.statements.ParameterNode;
import ast.statements.PrintNode;
import ast.statements.ReturnNode;
import ast.statements.StatementNode;
import ast.statements.WhileNode;
import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import types.ValueType;

/**
 * Parser descendente recursivo que construye el AST del programa.
 */
public class Parser {
    private static final EnumSet<TokenType> FIRST_GLOBAL_ELEMENT = EnumSet.of(
            TokenType.LA_FAENA,
            TokenType.UN_TAL,
            TokenType.A_MUERTE
    );
    private static final EnumSet<TokenType> FOLLOW_GLOBAL_ELEMENT = EnumSet.of(
            TokenType.UN_TAL,
            TokenType.A_MUERTE,
            TokenType.LA_FAENA,
            TokenType.EOF
    );    
    private static final EnumSet<TokenType> FIRST_STATEMENT = EnumSet.of(
            TokenType.UN_TAL,
            TokenType.A_MUERTE,
            TokenType.IDENTIFIER,
            TokenType.FIJATE_SI,
            TokenType.SEGUN_VEA,
            TokenType.SUELTA,
            TokenType.DI
    );
    private static final EnumSet<TokenType> FOLLOW_STATEMENT = EnumSet.of(
        TokenType.UN_TAL,
        TokenType.A_MUERTE,
        TokenType.IDENTIFIER,
        TokenType.FIJATE_SI,
        TokenType.SEGUN_VEA,
        TokenType.SUELTA,
        TokenType.DI,
        TokenType.RBRACE
    );
    private static final EnumSet<TokenType> FIRST_TYPE = EnumSet.of(
        TokenType.PAVOS,
        TokenType.APACHAS,
        TokenType.CARRO,
        TokenType.JURA,
        TokenType.LETRILLA
    );
    private static final EnumSet<TokenType> FIRST_EXPRESSION = EnumSet.of(
            TokenType.PLUS,
            TokenType.MINUS,
            TokenType.INT_LITERAL,
            TokenType.FLOAT_LITERAL,
            TokenType.CHAR_LITERAL,
            TokenType.STRING_LITERAL,
            TokenType.JURAO,
            TokenType.BULO,
            TokenType.IDENTIFIER,
            TokenType.LPAREN
    );
    private static final EnumSet<TokenType> FOLLOW_PARAMETER = EnumSet.of(
            TokenType.COMMA,
            TokenType.RPAREN,
            TokenType.EOF
    );
    private static final EnumSet<TokenType> FOLLOW_ARGUMENT = EnumSet.of(
            TokenType.COMMA,
            TokenType.RPAREN,
            TokenType.EOF
    );
    private static final EnumSet<TokenType> FOLLOW_EXPRESSION = EnumSet.of(
            TokenType.SEMICOLON,
            TokenType.COMMA,
            TokenType.RPAREN,
            TokenType.RBRACE,
            TokenType.EOF
    );

    private final Lexer lexer;
    private final List<ParserException> errors = new ArrayList<>();
    private Token current;
    private Token previous;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.current = lexer.readNextToken();
        this.previous = null;
    }

    // Devuelve todos los errores sintacticos registrados durante el parseo.
    public List<ParserException> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    // Indica si el parser detecto algun error recuperable.
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // Lee todo el programa y devuelve la raiz del AST.
    public ProgramNode parseProgram() {
        Token first = peek();
        List<GlobalElement> globals = new ArrayList<>();

        while (!isAtEnd()) {
            try {
                globals.add(parseGlobalElement());
            } catch (ParserException ex) {
                synchronize(FIRST_GLOBAL_ELEMENT, FOLLOW_GLOBAL_ELEMENT);
            }
        }

        return at(new ProgramNode(globals), first);
    }

    // Distingue entre declaracion global y definicion de funcion.
    private GlobalElement parseGlobalElement() {
        if (check(TokenType.LA_FAENA)) {
            return parseFunctionDecl();
        }

        if (check(TokenType.UN_TAL) || check(TokenType.A_MUERTE)) {
            DeclarationNode decl = parseDeclaration();
            consumeRecoverable(TokenType.SEMICOLON, "';' despues de la declaracion global",
                    union(FIRST_GLOBAL_ELEMENT, FOLLOW_GLOBAL_ELEMENT));
            return decl;
        }

        throw error(peek(), "una declaracion global o una definicion de funcion");
    }

    // Lee una lista de sentencias encerrada entre llaves.
    private BlockNode parseBlock() {
        Token lbrace = consume(TokenType.LBRACE, "'{' al inicio del bloque");
        List<StatementNode> statements = new ArrayList<>();

        while (!isBlockTerminator()) {
            try {
                statements.add(parseStatement());
            } catch (ParserException ex) {
                synchronize(FIRST_STATEMENT, FOLLOW_STATEMENT);
            }
        }

        consumeRecoverable(TokenType.RBRACE, "'}' al final del bloque",
                tokens(TokenType.Y_SI_NO, TokenType.EOF));
        return at(new BlockNode(statements), lbrace);
    }

    // Reconoce la siguiente sentencia del cuerpo de una funcion o bloque.
    private StatementNode parseStatement() {
        if (check(TokenType.UN_TAL) || check(TokenType.A_MUERTE)) {
            DeclarationNode decl = parseDeclaration();
            consumeRecoverable(TokenType.SEMICOLON, "';' despues de la declaracion",
                    union(FIRST_STATEMENT, FOLLOW_STATEMENT));
            return decl;
        }

        if (check(TokenType.IDENTIFIER)) {
            StatementNode identifierStatement = parseIdentifierStatement();
            consumeRecoverable(TokenType.SEMICOLON, "';' despues de la sentencia",
                    union(FIRST_STATEMENT, FOLLOW_STATEMENT));
            return identifierStatement;
        }

        if (check(TokenType.FIJATE_SI)) return parseIfStatement();
        if (check(TokenType.SEGUN_VEA)) return parseWhileStatement();

        if (check(TokenType.SUELTA)) {
            ReturnNode ret = parseReturnStatement();
            consumeRecoverable(TokenType.SEMICOLON, "';' despues de la sentencia 'suelta'",
                    union(FIRST_STATEMENT, FOLLOW_STATEMENT));
            return ret;
        }

        if (check(TokenType.DI)) {
            PrintNode print = parsePrintStatement();
            consumeRecoverable(TokenType.SEMICOLON, "';' despues de la sentencia 'di!'",
                    union(FIRST_STATEMENT, FOLLOW_STATEMENT));
            return print;
        }

        throw error(peek(), "una sentencia valida (declaracion, asignacion, llamada, 'fijateSi', 'segunVea', 'suelta' o 'di!')");
    }

    // Lee una declaracion de variable o constante.
    private DeclarationNode parseDeclaration() {
        boolean constant;
        Token keyword;

        if (match(TokenType.UN_TAL)) {
            constant = false;
            keyword = previous();
        } else if (match(TokenType.A_MUERTE)) {
            constant = true;
            keyword = previous();
        } else {
            throw error(peek(), "'unTal' o 'aMuerte' al inicio de una declaracion");
        }

        ValueType type = parseType();
        Token id = consume(TokenType.IDENTIFIER, "un identificador en la declaracion");

        ExpressionNode initializer = null;

        if (constant) {
            consume(TokenType.ASSIGN, "'=' (una constante 'aMuerte' debe inicializarse en su declaracion)");
            initializer = parseExpression();
        } else if (match(TokenType.ASSIGN)) {
            initializer = parseExpression();
        }

        return at(new DeclarationNode(constant, type, id.getLexeme(), initializer), keyword);
    }

    // Distingue entre asignacion y llamada cuando la sentencia empieza por identificador.
    private StatementNode parseIdentifierStatement() {
        Token id = consume(TokenType.IDENTIFIER, "un identificador al inicio de la sentencia");

        if (match(TokenType.ASSIGN)) {
            ExpressionNode expr = parseExpression();
            return at(new AssignmentNode(id.getLexeme(), expr), id);
        }

        if (match(TokenType.LPAREN)) {
            List<ExpressionNode> args = parseExpressionList("los argumentos de la llamada a funcion");
            consumeRecoverable(TokenType.RPAREN, "')' al final de la llamada a funcion",
                    tokens(TokenType.SEMICOLON, TokenType.EOF));
            return at(new CallStatementNode(new CallExprNode(id.getLexeme(), args)), id);
        }

        throw error(peek(), "'=' en la asignacion o '(' en la llamada a funcion");
    }

    // Lee una sentencia de impresion di!(...).
    private PrintNode parsePrintStatement() {
        Token kw = consume(TokenType.DI, "'di!' al inicio de la sentencia de impresion");
        consume(TokenType.LPAREN, "'(' despues de 'di!'");

        List<ExpressionNode> args = parseExpressionList("los argumentos de 'di!'");
        consumeRecoverable(TokenType.RPAREN, "')' al final de los argumentos de 'di!'",
                tokens(TokenType.SEMICOLON, TokenType.EOF));
        return at(new PrintNode(args), kw);
    }

    // Lee una sentencia condicional con bloque opcional ySiNo.
    private IfNode parseIfStatement() {
        Token kw = consume(TokenType.FIJATE_SI, "'fijateSi' al inicio del condicional");
        consume(TokenType.LPAREN, "'(' despues de 'fijateSi'");
        ExpressionNode condition = parseExpression();
        consumeRecoverable(TokenType.RPAREN, "')' despues de la condicion del 'fijateSi'",
                tokens(TokenType.LBRACE));

        BlockNode thenBlock = parseBlock();
        BlockNode elseBlock = null;

        if (match(TokenType.Y_SI_NO)) {
            elseBlock = parseBlock();
        }

        return at(new IfNode(condition, thenBlock, elseBlock), kw);
    }

    // Lee un bucle segunVea con su condicion y cuerpo.
    private WhileNode parseWhileStatement() {
        Token kw = consume(TokenType.SEGUN_VEA, "'segunVea' al inicio del bucle");
        consume(TokenType.LPAREN, "'(' despues de 'segunVea'");
        ExpressionNode condition = parseExpression();
        consumeRecoverable(TokenType.RPAREN, "')' despues de la condicion del 'segunVea'",
                tokens(TokenType.LBRACE));
        BlockNode body = parseBlock();

        return at(new WhileNode(condition, body), kw);
    }

    // Lee una definicion completa de funcion.
    private FunctionDeclNode parseFunctionDecl() {
        Token kw = consume(TokenType.LA_FAENA, "'laFaena' al inicio de la definicion de funcion");

        ValueType returnType = match(TokenType.NA_DE_NA) ? ValueType.NA_DE_NA : parseType();

        Token name = consume(TokenType.IDENTIFIER, "el nombre de la funcion");
        consume(TokenType.LPAREN, "'(' despues del nombre de la funcion");

        List<ParameterNode> params = parseParameterList();
        consumeRecoverable(TokenType.RPAREN, "')' al final de la lista de parametros",
                tokens(TokenType.LBRACE, TokenType.EOF));
        BlockNode body = parseBlock();

        return at(new FunctionDeclNode(returnType, name.getLexeme(), params, body), kw);
    }

    // Lee un parametro de funcion: tipo e identificador.
    private ParameterNode parseParameter() {
        ValueType type = parseType();
        Token id = consume(TokenType.IDENTIFIER, "el identificador del parametro");
        return at(new ParameterNode(type, id.getLexeme()), id);
    }

    // Lee una sentencia suelta, con valor opcional.
    private ReturnNode parseReturnStatement() {
        Token kw = consume(TokenType.SUELTA, "'suelta' al inicio de la sentencia de retorno");
        ExpressionNode value = null;

        if (!check(TokenType.SEMICOLON)) {
            value = parseExpression();
        }

        return at(new ReturnNode(value), kw);
    }

    // Lee uno de los tipos basicos del lenguaje.
    private ValueType parseType() {
        if (match(TokenType.PAVOS)) return ValueType.PAVOS;
        if (match(TokenType.APACHAS)) return ValueType.APACHAS;
        if (match(TokenType.CARRO)) return ValueType.CARRO;
        if (match(TokenType.JURA)) return ValueType.JURA;
        if (match(TokenType.LETRILLA)) return ValueType.LETRILLA;

        throw error(peek(), "un tipo valido ('pavos', 'aPachas', 'carro', 'letrilla' o 'jura')");
    }

    // Punto de entrada para leer expresiones.
    private ExpressionNode parseExpression() {
        return parseOr();
    }

    // Lee operaciones logicas OR.
    private ExpressionNode parseOr() {
        ExpressionNode expr = parseAnd();

        while (match(TokenType.OR)) {
            Token opTok = previous();
            ExpressionNode right = parseAnd();
            expr = at(new BinaryExprNode(expr, opTok.getLexeme(), right), opTok);
        }
        return expr;
    }

    // Lee operaciones logicas AND.
    private ExpressionNode parseAnd() {
        ExpressionNode expr = parseComparison();

        while (match(TokenType.AND)) {
            Token opTok = previous();
            ExpressionNode right = parseComparison();
            expr = at(new BinaryExprNode(expr, opTok.getLexeme(), right), opTok);
        }
        return expr;
    }

    // Lee comparaciones como ==, <, >, <= y >=.
    private ExpressionNode parseComparison() {
        ExpressionNode expr = parseAddition();

        while (match(TokenType.EQUAL_EQUAL, TokenType.LESS, TokenType.GREATER,
                TokenType.LESS_EQUAL, TokenType.GREATER_EQUAL)) {
            Token opTok = previous();
            ExpressionNode right = parseAddition();
            expr = at(new BinaryExprNode(expr, opTok.getLexeme(), right), opTok);
        }
        return expr;
    }

    // Lee sumas y restas.
    private ExpressionNode parseAddition() {
        ExpressionNode expr = parseMultiplication();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token opTok = previous();
            ExpressionNode right = parseMultiplication();
            expr = at(new BinaryExprNode(expr, opTok.getLexeme(), right), opTok);
        }
        return expr;
    }

    // Lee multiplicaciones y divisiones.
    private ExpressionNode parseMultiplication() {
        ExpressionNode expr = parseUnary();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token opTok = previous();
            ExpressionNode right = parseUnary();
            expr = at(new BinaryExprNode(expr, opTok.getLexeme(), right), opTok);
        }
        return expr;
    }

    // Lee operadores unarios + y -.
    private ExpressionNode parseUnary() {
        if (match(TokenType.PLUS, TokenType.MINUS)) {
            Token opTok = previous();
            ExpressionNode right = parseUnary();
            return at(new UnaryExprNode(opTok.getLexeme(), right), opTok);
        }
        return parsePrimary();
    }

    // Lee literales, identificadores, llamadas y expresiones entre parentesis.
    private ExpressionNode parsePrimary() {
        if (match(TokenType.INT_LITERAL, TokenType.FLOAT_LITERAL,
          TokenType.CHAR_LITERAL, TokenType.STRING_LITERAL,
          TokenType.JURAO, TokenType.BULO)) {
            Token lit = previous();
            return at(new LiteralExprNode(lit.getType(), lit.getLexeme()), lit);
        }

        if (match(TokenType.IDENTIFIER)) {
            Token id = previous();

            if (match(TokenType.LPAREN)) {
                List<ExpressionNode> args = parseExpressionList("los argumentos de la llamada a funcion");
                consumeRecoverable(TokenType.RPAREN, "')' al final de la llamada a funcion",
                        FOLLOW_EXPRESSION);
                return at(new CallExprNode(id.getLexeme(), args), id);
            }

            return at(new IdentifierExprNode(id.getLexeme()), id);
        }

        if (match(TokenType.LPAREN)) {
            ExpressionNode expr = parseExpression();
            consumeRecoverable(TokenType.RPAREN, "')' al cerrar la expresion entre parentesis",
                    FOLLOW_EXPRESSION);
            return expr;
        }

        throw error(peek(), "un literal, un identificador o una expresion entre parentesis");
    }

    // Avanza si el token actual coincide con alguno de los tipos dados.
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Consume el token esperado o genera un error sintactico.
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    // Recupera delimitadores frecuentes si el siguiente token encaja con FOLLOW.
    private void consumeRecoverable(TokenType type, String message, EnumSet<TokenType> followSet) {
        if (check(type)) {
            advance();
            return;
        }

        if (followSet.contains(peek().getType())) {
            error(peek(), message);
            return;
        }

        throw error(peek(), message);
    }

    // Comprueba el tipo del token actual sin consumirlo.
    private boolean check(TokenType type) {
        if (isAtEnd()) return type == TokenType.EOF;
        return peek().getType() == type;
    }

    // Consume el token actual y carga el siguiente.
    private Token advance() {
        Token token = current;
        previous = token;
        if (!isAtEnd()) {
            current = lexer.readNextToken();
        }
        return token;
    }

    // Indica si el parser ya llego al final del fichero.
    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    // Devuelve el token actual.
    private Token peek() {
        return current;
    }

    // Devuelve el ultimo token consumido.
    private Token previous() {
        return previous;
    }

    // Salta tokens hasta un punto compatible con FIRST/FOLLOW del contexto actual.
    private void synchronize(EnumSet<TokenType> firstSet, EnumSet<TokenType> followSet) {
        while (!isAtEnd()) {
            TokenType currentType = peek().getType();

            if (firstSet.contains(currentType) || followSet.contains(currentType)) {
                return;
            }

            if (currentType == TokenType.SEMICOLON) {
                advance();
                return;
            }

            if (currentType == TokenType.LBRACE) {
                skipCurrentBlock();
                continue;
            }

            advance();
        }
    }

    // Detecta el fin de un bloque o un token que sugiere una llave de cierre omitida.
    private boolean isBlockTerminator() {
        return check(TokenType.RBRACE) || check(TokenType.Y_SI_NO) || isAtEnd();
    }

    // Descarta un bloque completo para no mezclar sentencias de un contexto roto.
    private void skipCurrentBlock() {
        int depth = 0;

        while (!isAtEnd()) {
            if (check(TokenType.LBRACE)) {
                depth++;
            } else if (check(TokenType.RBRACE)) {
                depth--;
            }

            advance();

            if (depth <= 0) {
                return;
            }
        }
    }

    // Lee una lista separada por comas de parametros.
    private List<ParameterNode> parseParameterList() {
        List<ParameterNode> params = new ArrayList<>();

        while (!check(TokenType.RPAREN) && !isAtEnd()) {
            try {
                params.add(parseParameter());
            } catch (ParserException ex) {
                synchronize(FIRST_TYPE, FOLLOW_PARAMETER);
            }

            if (check(TokenType.RPAREN) || isAtEnd()) {
                break;
            }

            consumeRecoverable(TokenType.COMMA, "',' entre parametros",
                    union(FIRST_TYPE, FOLLOW_PARAMETER));
        }

        return params;
    }

    // Lee una lista separada por comas de expresiones.
    private List<ExpressionNode> parseExpressionList(String contextDescription) {
        List<ExpressionNode> expressions = new ArrayList<>();

        while (!check(TokenType.RPAREN) && !isAtEnd()) {
            try {
                expressions.add(parseExpression());
            } catch (ParserException ex) {
                synchronize(FIRST_EXPRESSION, FOLLOW_ARGUMENT);
            }

            if (check(TokenType.RPAREN) || isAtEnd()) {
                break;
            }

            consumeRecoverable(TokenType.COMMA, "',' entre " + contextDescription,
                    union(FIRST_EXPRESSION, FOLLOW_ARGUMENT));
        }

        return expressions;
    }

    // Crea una excepcion sintactica con el token actual.
    private ParserException error(Token token, String expected) {
        ParserException parserException = new ParserException(token, expected);
        errors.add(parserException);
        return parserException;
    }

    // Guarda la posicion de origen en el nodo del AST.
    private <T extends ASTNode> T at(T node, Token token) {
        node.setPosition(token.getLine(), token.getColumn());
        return node;
    }

    // Crea un conjunto de tokens a partir de literales.
    private static EnumSet<TokenType> tokens(TokenType... types) {
        EnumSet<TokenType> tokenSet = EnumSet.noneOf(TokenType.class);
        Collections.addAll(tokenSet, types);
        return tokenSet;
    }

    // Une conjuntos FIRST/FOLLOW para reglas recuperables.
    private static EnumSet<TokenType> union(EnumSet<TokenType> firstSet, EnumSet<TokenType> secondSet) {
        EnumSet<TokenType> union = EnumSet.copyOf(firstSet);
        union.addAll(secondSet);
        return union;
    }
}
