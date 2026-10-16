package ast.statements;

import java.util.List;
import ast.ASTNode;
import ast.GlobalElement;
import types.ValueType;

/**
 * Declaracion de funcion con firma y cuerpo.
 */
public class FunctionDeclNode extends ASTNode implements GlobalElement {
    private final ValueType returnType;
    private final String name;
    private final List<ParameterNode> parameters;
    private final BlockNode body;

    public FunctionDeclNode(ValueType returnType, String name, List<ParameterNode> parameters, BlockNode body) {
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    // Devuelve el tipo de retorno declarado.
    public ValueType getReturnType() { return returnType; }

    // Devuelve el nombre de la funcion.
    public String getName() { return name; }

    // Devuelve los parametros de la funcion.
    public List<ParameterNode> getParameters() { return parameters; }

    // Devuelve el cuerpo de la funcion.
    public BlockNode getBody() { return body; }
}
