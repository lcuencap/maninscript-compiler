package ir;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Instruccion IR que imprime una lista de operandos.
 */
public class PrintInstruction implements IRInstruction {
    private final List<IROperand> arguments;

    public PrintInstruction(List<IROperand> arguments) {
        this.arguments = List.copyOf(arguments);
    }

    // Devuelve los operandos enviados a impresion.
    public List<IROperand> getArguments() {
        return arguments;
    }

    @Override
    public String format() {
        String args = arguments.stream()
                .map(IROperand::format)
                .collect(Collectors.joining(", "));
        return "print " + args;
    }
}
