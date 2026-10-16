package ir;

import java.util.List;

/**
 * Aplica optimizaciones locales sencillas sobre un programa IR.
 */
public class IROptimizer {
    // Aplica optimizaciones locales sencillas sobre un programa IR.
    public IRProgram optimize(IRProgram program) {
        IRProgram optimizedProgram = new IRProgram();

        for (IRVariable globalVariable : program.getGlobalVariables()) {
            optimizedProgram.addGlobalVariable(globalVariable);
        }

        copyOptimizedInstructions(program.getGlobalInstructions(), optimizedProgram);

        for (IRFunction function : program.getFunctions()) {
            optimizedProgram.addFunction(optimizeFunction(function));
        }

        return optimizedProgram;
    }

    private IRFunction optimizeFunction(IRFunction function) {
        IRFunction optimizedFunction = new IRFunction(
                function.getName(),
                function.getReturnType(),
                function.getParameters()
        );

        for (IRVariable localVariable : function.getLocalVariables()) {
            optimizedFunction.addLocalVariable(localVariable);
        }

        copyOptimizedInstructions(function.getInstructions(), optimizedFunction);
        return optimizedFunction;
    }

    private void copyOptimizedInstructions(List<IRInstruction> instructions, IRProgram targetProgram) {
        for (IRInstruction instruction : removeDeadCode(instructions)) {
            targetProgram.addGlobalInstruction(instruction);
        }
    }

    private void copyOptimizedInstructions(List<IRInstruction> instructions, IRFunction targetFunction) {
        for (IRInstruction instruction : removeDeadCode(instructions)) {
            targetFunction.addInstruction(instruction);
        }
    }

    // Elimina instrucciones inalcanzables tras return/goto hasta la siguiente etiqueta.
    private List<IRInstruction> removeDeadCode(List<IRInstruction> instructions) {
        java.util.ArrayList<IRInstruction> optimized = new java.util.ArrayList<>();
        boolean skippingDeadCode = false;

        for (IRInstruction instruction : instructions) {
            if (instruction instanceof LabelInstruction) {
                skippingDeadCode = false;
                optimized.add(instruction);
                continue;
            }

            if (skippingDeadCode) {
                continue;
            }

            optimized.add(instruction);
            if (isTerminal(instruction)) {
                skippingDeadCode = true;
            }
        }

        return optimized;
    }

    private boolean isTerminal(IRInstruction instruction) {
        return instruction instanceof ReturnInstruction
                || instruction instanceof GotoInstruction;
    }
}
