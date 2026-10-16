package mips;

import ir.IRInstruction;

/**
 * Excepcion lanzada cuando falla la generacion de codigo MIPS.
 */
public class MipsGenerationException extends RuntimeException {
    public MipsGenerationException(String message) {
        super("Error generando MIPS: " + message);
    }

    public MipsGenerationException(IRInstruction instruction, String message) {
        super(buildMessage(instruction, message));
    }

    private static String buildMessage(IRInstruction instruction, String message) {
        if (instruction == null) {
            return "Error generando MIPS: " + message;
        }

        return "Error generando MIPS: " + message
                + ". Instruccion IR: " + instruction.format();
    }
}
