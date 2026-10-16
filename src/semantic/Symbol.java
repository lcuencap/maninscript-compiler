package semantic;

import types.ValueType;

/**
 * Representa una variable o constante registrada en una tabla de simbolos.
 */
public class Symbol {
    private final String name;
    private final ValueType type;
    private final boolean isConstant;

    public Symbol(String name, ValueType type, boolean isConstant) {
        this.name = name;
        this.type = type;
        this.isConstant = isConstant;
    }

    // Devuelve el nombre del simbolo.
    public String getName() {
        return name;
    }

    // Devuelve el tipo asociado al simbolo.
    public ValueType getType() {
        return type;
    }

    // Indica si el simbolo representa una constante.
    public boolean isConstant() {
        return isConstant;
    }
}
