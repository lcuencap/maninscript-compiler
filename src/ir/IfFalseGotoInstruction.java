package ir;

/**
 * Instruccion IR que salta cuando la condicion es falsa.
 */
public class IfFalseGotoInstruction implements IRInstruction {
    private final IROperand condition;
    private final LabelOperand target;

    public IfFalseGotoInstruction(IROperand condition, LabelOperand target) {
        this.condition = condition;
        this.target = target;
    }

    // Devuelve la condicion evaluada antes del salto.
    public IROperand getCondition() {
        return condition;
    }

    // Devuelve la etiqueta destino del salto.
    public LabelOperand getTarget() {
        return target;
    }

    @Override
    public String format() {
        return "ifFalse " + condition.format() + " goto " + target.format();
    }
}
