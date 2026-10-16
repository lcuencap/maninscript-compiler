package ir;

/**
 * Instruccion IR para una operacion unaria.
 */
public class UnaryInstruction implements IRInstruction {
    private final IROperand target;
    private final String operator;
    private final IROperand value;

    public UnaryInstruction(IROperand target, String operator, IROperand value) {
        this.target = target;
        this.operator = operator;
        this.value = value;
    }

    // Devuelve el operando destino del resultado.
    public IROperand getTarget() {
        return target;
    }

    // Devuelve el operador unario aplicado.
    public String getOperator() {
        return operator;
    }

    // Devuelve el operando fuente.
    public IROperand getValue() {
        return value;
    }

    @Override
    public String format() {
        return target.format() + " = " + operator + value.format();
    }
}
