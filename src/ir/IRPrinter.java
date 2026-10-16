package ir;

import java.util.stream.Collectors;

/**
 * Convierte un programa IR a una representacion textual legible.
 */
public class IRPrinter {
    // Convierte un programa intermedio completo a texto.
    public String print(IRProgram program) {
        StringBuilder builder = new StringBuilder();

        if (!program.getGlobalInstructions().isEmpty()) {
            builder.append("globals").append(System.lineSeparator());
            for (IRInstruction instruction : program.getGlobalInstructions()) {
                appendInstruction(builder, instruction);
            }
            builder.append("end globals").append(System.lineSeparator());
        }

        for (IRFunction function : program.getFunctions()) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            appendFunction(builder, function);
        }

        return builder.toString();
    }

    private void appendFunction(StringBuilder builder, IRFunction function) {
        String parameters = function.getParameters().stream()
                .map(IRParameter::format)
                .collect(Collectors.joining(", "));

        builder.append("function ")
                .append(function.getName())
                .append("(")
                .append(parameters)
                .append(") : ")
                .append(function.getReturnType())
                .append(System.lineSeparator());

        for (IRInstruction instruction : function.getInstructions()) {
            appendInstruction(builder, instruction);
        }

        builder.append("end function").append(System.lineSeparator());
    }

    private void appendInstruction(StringBuilder builder, IRInstruction instruction) {
        if (instruction instanceof LabelInstruction) {
            builder.append(instruction.format()).append(System.lineSeparator());
            return;
        }

        builder.append("  ").append(instruction.format()).append(System.lineSeparator());
    }
}
