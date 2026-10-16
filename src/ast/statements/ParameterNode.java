package ast.statements;

import ast.ASTNode;
import types.ValueType;

/**
 * Parametro formal declarado en una funcion.
 */
public class ParameterNode extends ASTNode {
    private final ValueType type;
    private final String identifier;

    public ParameterNode(ValueType type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    // Devuelve el tipo del parametro.
    public ValueType getType() { return type; }

    // Devuelve el nombre del parametro.
    public String getIdentifier() { return identifier; }
}
