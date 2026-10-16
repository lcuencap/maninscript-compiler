package ir;

import types.ValueType;

/**
 * Representa un parametro formal dentro de una funcion IR.
 */
public class IRParameter {
    private final ValueType type;
    private final String name;

    public IRParameter(ValueType type, String name) {
        this.type = type;
        this.name = name;
    }

    // Devuelve el tipo semantico del parametro.
    public ValueType getType() {
        return type;
    }

    // Devuelve el nombre del parametro.
    public String getName() {
        return name;
    }

    // Representa el parametro en formato legible.
    public String format() {
        return type + " " + name;
    }
}
