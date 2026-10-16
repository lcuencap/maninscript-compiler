package ir;

import types.ValueType;

/**
 * Describe una variable nombrada del programa IR junto a su tipo.
 */
public class IRVariable {
    private final ValueType type;
    private final String name;

    public IRVariable(ValueType type, String name) {
        this.type = type;
        this.name = name;
    }

    // Devuelve el tipo semantico asociado a la variable.
    public ValueType getType() {
        return type;
    }

    // Devuelve el nombre de la variable.
    public String getName() {
        return name;
    }
}
