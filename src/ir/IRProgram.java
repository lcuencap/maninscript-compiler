package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contenedor del codigo intermedio global y de las funciones.
 */
public class IRProgram {
    private final List<IRVariable> globalVariables = new ArrayList<>();
    private final List<IRInstruction> globalInstructions = new ArrayList<>();
    private final List<IRFunction> functions = new ArrayList<>();

    // Registra una variable global conocida por el programa IR.
    public void addGlobalVariable(IRVariable globalVariable) {
        if (containsVariable(globalVariables, globalVariable.getName())) {
            return;
        }

        globalVariables.add(globalVariable);
    }

    // Registra una instruccion de inicializacion global.
    public void addGlobalInstruction(IRInstruction instruction) {
        globalInstructions.add(instruction);
    }

    // Registra una funcion traducida a codigo intermedio.
    public void addFunction(IRFunction function) {
        functions.add(function);
    }

    // Devuelve las variables globales registradas.
    public List<IRVariable> getGlobalVariables() {
        return Collections.unmodifiableList(globalVariables);
    }

    // Devuelve las instrucciones de ambito global.
    public List<IRInstruction> getGlobalInstructions() {
        return Collections.unmodifiableList(globalInstructions);
    }

    // Devuelve las funciones del programa intermedio.
    public List<IRFunction> getFunctions() {
        return Collections.unmodifiableList(functions);
    }

    private boolean containsVariable(List<IRVariable> variables, String name) {
        for (IRVariable variable : variables) {
            if (variable.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }
}
