package ast.statements;

import ast.GlobalElement;
import ast.expressions.ExpressionNode;
import types.ValueType;

/**
 * Declaracion de variable o constante.
 */
public class DeclarationNode extends StatementNode implements GlobalElement {
    private final boolean constant;
    private final ValueType type;
    private final String identifier;
    private final ExpressionNode initializer;

    public DeclarationNode(boolean constant, ValueType type, String identifier, ExpressionNode initializer) {
        this.constant = constant;
        this.type = type;
        this.identifier = identifier;
        this.initializer = initializer;
    }

    // Indica si la declaracion es constante.
    public boolean isConstant() { return constant; }

    // Devuelve el tipo declarado.
    public ValueType getType() { return type; }

    // Devuelve el nombre declarado.
    public String getIdentifier() { return identifier; }

    // Devuelve la expresion inicial, si existe.
    public ExpressionNode getInitializer() { return initializer; }
}
