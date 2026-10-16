package ir;

import types.ValueType;

/**
 * Operando temporal generado durante la traduccion a IR.
 */
public class TempOperand implements IROperand {
    private final String name;
    private final ValueType type;

    public TempOperand(String name) {
        this(name, null);
    }

    public TempOperand(String name, ValueType type) {
        this.name = name;
        this.type = type;
    }

    // Devuelve el nombre interno del temporal.
    public String getName() {
        return name;
    }

    // Devuelve el tipo semantico del temporal, si se conoce.
    public ValueType getType() {
        return type;
    }

    @Override
    public String format() {
        return name;
    }
}
