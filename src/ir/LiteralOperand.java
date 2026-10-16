package ir;

import types.ValueType;

/**
 * Operando literal usado por las instrucciones IR.
 */
public class LiteralOperand implements IROperand {
    private final String value;
    private final ValueType type;

    public LiteralOperand(String value) {
        this(value, null);
    }

    public LiteralOperand(String value, ValueType type) {
        this.value = value;
        this.type = type;
    }

    // Devuelve el valor textual del literal.
    public String getValue() {
        return value;
    }

    // Devuelve el tipo semantico del literal, si se conoce.
    public ValueType getType() {
        return type;
    }

    @Override
    public String format() {
        return value;
    }
}
