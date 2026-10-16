package ir;

/**
 * Instruccion IR para una operacion binaria.
 */
public class BinaryInstruction implements IRInstruction {
    private final IROperand target;
    private final IROperand left;
    private final String operator;
    private final IROperand right;

    public BinaryInstruction(IROperand target, IROperand left, String operator, IROperand right) {
        this.target = target;
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    // Devuelve el operando destino del resultado.
    public IROperand getTarget() {
        return target;
    }

    // Devuelve el operando izquierdo.
    public IROperand getLeft() {
        return left;
    }

    // Devuelve el operador binario.
    public String getOperator() {
        return operator;
    }

    // Devuelve el operando derecho.
    public IROperand getRight() {
        return right;
    }

    @Override
    public String format() {
        return target.format() + " = " + left.format() + " " + operator + " " + right.format();
    }
}
