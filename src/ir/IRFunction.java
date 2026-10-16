package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import types.ValueType;

/**
 * Representa una funcion traducida a codigo intermedio.
 */
public class IRFunction {
    private final String name;
    private final ValueType returnType;
    private final List<IRParameter> parameters;
    private final List<IRVariable> localVariables = new ArrayList<>();
    private final List<IRInstruction> instructions = new ArrayList<>();

    public IRFunction(String name, ValueType returnType, List<IRParameter> parameters) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = List.copyOf(parameters);
    }

    // Devuelve el nombre de la funcion.
    public String getName() {
        return name;
    }

    // Devuelve el tipo de retorno declarado.
    public ValueType getReturnType() {
        return returnType;
    }

    // Devuelve los parametros de la funcion.
    public List<IRParameter> getParameters() {
        return parameters;
    }

    // Registra una variable local declarada dentro de la funcion.
    public void addLocalVariable(IRVariable localVariable) {
        if (containsVariable(localVariables, localVariable.getName())) {
            return;
        }

        localVariables.add(localVariable);
    }

    // Devuelve las variables locales conocidas de la funcion.
    public List<IRVariable> getLocalVariables() {
        return Collections.unmodifiableList(localVariables);
    }

    // Registra una instruccion TAC dentro de la funcion.
    public void addInstruction(IRInstruction instruction) {
        instructions.add(instruction);
    }

    // Devuelve las instrucciones de la funcion.
    public List<IRInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
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
