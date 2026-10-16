package ast.expressions;

import lexer.TokenType;

/**
 * Expresion literal con su tipo de token original.
 */
public class LiteralExprNode extends ExpressionNode {
    private final TokenType literalType;
    private final String value;

    public LiteralExprNode(TokenType literalType, String value) {
        this.literalType = literalType;
        this.value = value;
    }

    // Devuelve el tipo de token literal.
    public TokenType getLiteralType() { return literalType; }

    // Devuelve el valor textual del literal.
    public String getValue() { return value; }
}
