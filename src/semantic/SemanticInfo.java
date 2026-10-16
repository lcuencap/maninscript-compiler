package semantic;

import ast.expressions.ExpressionNode;
import ast.statements.AssignmentNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import types.ValueType;

/**
 * Contiene la informacion derivada del analisis semantico.
 */
public class SemanticInfo {
    private final Map<String, FunctionSymbol> functions;
    private final Map<String, Symbol> globalSymbols;
    private final Map<AssignmentNode, ValueType> assignmentTargetTypes;
    private final Map<ExpressionNode, ValueType> expressionTypes;

    public SemanticInfo(Map<String, FunctionSymbol> functions,
                        Map<String, Symbol> globalSymbols,
                        Map<AssignmentNode, ValueType> assignmentTargetTypes,
                        Map<ExpressionNode, ValueType> expressionTypes) {
        this.functions = Collections.unmodifiableMap(new HashMap<>(functions));
        this.globalSymbols = Collections.unmodifiableMap(new HashMap<>(globalSymbols));

        IdentityHashMap<AssignmentNode, ValueType> assignmentTargetTypeCopy = new IdentityHashMap<>();
        assignmentTargetTypeCopy.putAll(assignmentTargetTypes);
        this.assignmentTargetTypes = Collections.unmodifiableMap(assignmentTargetTypeCopy);

        IdentityHashMap<ExpressionNode, ValueType> expressionTypeCopy = new IdentityHashMap<>();
        expressionTypeCopy.putAll(expressionTypes);
        this.expressionTypes = Collections.unmodifiableMap(expressionTypeCopy);
    }

    // Devuelve la firma semantica de una funcion declarada.
    public FunctionSymbol getFunction(String name) {
        return functions.get(name);
    }

    // Devuelve todas las funciones conocidas tras el analisis semantico.
    public Map<String, FunctionSymbol> getFunctions() {
        return functions;
    }

    // Devuelve el simbolo semantico de una variable global.
    public Symbol getGlobalSymbol(String name) {
        return globalSymbols.get(name);
    }

    // Devuelve todas las variables globales conocidas tras el analisis.
    public Map<String, Symbol> getGlobalSymbols() {
        return globalSymbols;
    }

    // Devuelve el tipo conocido del destino de una asignacion concreta.
    public ValueType getAssignmentTargetType(AssignmentNode assignment) {
        return assignmentTargetTypes.get(assignment);
    }

    // Indica si se registro tipo para el destino de esa asignacion.
    public boolean hasAssignmentTargetType(AssignmentNode assignment) {
        return assignmentTargetTypes.containsKey(assignment);
    }

    // Devuelve el tipo calculado para una expresion concreta del AST.
    public ValueType getExpressionType(ExpressionNode expression) {
        return expressionTypes.get(expression);
    }

    // Indica si el analizador registro tipo para esa expresion del AST.
    public boolean hasExpressionType(ExpressionNode expression) {
        return expressionTypes.containsKey(expression);
    }
}
