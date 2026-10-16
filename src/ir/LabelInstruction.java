package ir;

/**
 * Instruccion IR que declara una etiqueta.
 */
public class LabelInstruction implements IRInstruction {
    private final LabelOperand label;

    public LabelInstruction(LabelOperand label) {
        this.label = label;
    }

    // Devuelve la etiqueta declarada por la instruccion.
    public LabelOperand getLabel() {
        return label;
    }

    @Override
    public String format() {
        return label.format() + ":";
    }
}
