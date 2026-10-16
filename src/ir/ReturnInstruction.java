package ir;

/**
 * Instruccion IR de retorno con valor opcional.
 */
public class ReturnInstruction implements IRInstruction {
    private final IROperand value;

    public ReturnInstruction(IROperand value) {
        this.value = value;
    }

    // Devuelve el operando retornado, si existe.
    public IROperand getValue() {
        return value;
    }

    @Override
    public String format() {
        if (value == null) {
            return "return";
        }

        return "return " + value.format();
    }
}
