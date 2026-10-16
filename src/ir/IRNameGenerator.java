package ir;

import types.ValueType;

/**
 * Genera nombres unicos para temporales y etiquetas.
 */
public class IRNameGenerator {
    private int nextTemp = 0;
    private int nextLabel = 0;

    // Crea un nuevo temporal TAC.
    public TempOperand newTemp() {
        return newTemp(null);
    }

    // Crea un nuevo temporal TAC tipado.
    public TempOperand newTemp(ValueType type) {
        return new TempOperand("t" + nextTemp++, type);
    }

    // Crea una nueva etiqueta TAC.
    public LabelOperand newLabel() {
        return new LabelOperand("L" + nextLabel++);
    }
}
