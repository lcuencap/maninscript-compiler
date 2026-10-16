package ir;

/**
 * Instruccion IR de salto incondicional.
 */
public class GotoInstruction implements IRInstruction {
    private final LabelOperand target;

    public GotoInstruction(LabelOperand target) {
        this.target = target;
    }

    // Devuelve la etiqueta de salto.
    public LabelOperand getTarget() {
        return target;
    }

    @Override
    public String format() {
        return "goto " + target.format();
    }
}
