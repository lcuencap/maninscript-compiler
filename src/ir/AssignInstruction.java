package ir;

/**
 * Instruccion IR que copia un valor en un destino.
 */
public class AssignInstruction implements IRInstruction {
    private final IROperand target;
    private final IROperand value;

    public AssignInstruction(IROperand target, IROperand value) {
        this.target = target;
        this.value = value;
    }

    // Devuelve el operando destino de la asignacion.
    public IROperand getTarget() {
        return target;
    }

    // Devuelve el valor que se copia al destino.
    public IROperand getValue() {
        return value;
    }

    @Override
    public String format() {
        return target.format() + " = " + value.format();
    }
}
