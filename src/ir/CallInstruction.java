package ir;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Instruccion IR que representa una llamada a funcion.
 */
public class CallInstruction implements IRInstruction {
    private final IROperand target;
    private final String functionName;
    private final List<IROperand> arguments;

    public CallInstruction(IROperand target, String functionName, List<IROperand> arguments) {
        this.target = target;
        this.functionName = functionName;
        this.arguments = List.copyOf(arguments);
    }

    // Devuelve el destino donde se almacena el retorno, si existe.
    public IROperand getTarget() {
        return target;
    }

    // Devuelve el nombre de la funcion invocada.
    public String getFunctionName() {
        return functionName;
    }

    // Devuelve los argumentos evaluados de la llamada.
    public List<IROperand> getArguments() {
        return arguments;
    }

    @Override
    public String format() {
        String args = arguments.stream()
                .map(IROperand::format)
                .collect(Collectors.joining(", "));

        if (target == null) {
            return "call " + functionName + "(" + args + ")";
        }

        return target.format() + " = call " + functionName + "(" + args + ")";
    }
}
