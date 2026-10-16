package ir;

import types.ValueType;

/**
 * Operando que referencia una variable por nombre.
 */
public class VariableOperand implements IROperand {
    private final String name;
    private final ValueType type;

    public VariableOperand(String name) {
        this(name, null);
    }

    public VariableOperand(String name, ValueType type) {
        this.name = name;
        this.type = type;
    }

    // Devuelve el nombre de la variable.
    public String getName() {
        return name;
    }

    // Devuelve el tipo conocido de la variable, si se tiene.
    public ValueType getType() {
        return type;
    }

    @Override
    public String format() {
        return name;
    }
}
