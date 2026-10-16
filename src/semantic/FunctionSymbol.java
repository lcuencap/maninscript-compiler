package semantic;

import java.util.List;
import types.ValueType;

/**
 * Describe la firma semantica de una funcion declarada.
 */
public class FunctionSymbol {
    private final String name;
    private final ValueType returnType;
    private final List<ValueType> parameterTypes;

    public FunctionSymbol(String name, ValueType returnType, List<ValueType> parameterTypes) {
        this.name = name;
        this.returnType = returnType;
        this.parameterTypes = List.copyOf(parameterTypes);
    }

    // Devuelve el nombre de la funcion.
    public String getName() {
        return name;
    }

    // Devuelve el tipo de retorno de la funcion.
    public ValueType getReturnType() {
        return returnType;
    }

    // Devuelve los tipos esperados de los parametros.
    public List<ValueType> getParameterTypes() {
        return parameterTypes;
    }
}
