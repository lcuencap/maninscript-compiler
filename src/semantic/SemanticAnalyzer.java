package semantic;

import ast.ASTNode;
import ast.GlobalElement;
import ast.ProgramNode;
import ast.expressions.BinaryExprNode;
import ast.expressions.CallExprNode;
import ast.expressions.ExpressionNode;
import ast.expressions.IdentifierExprNode;
import ast.expressions.LiteralExprNode;
import ast.expressions.UnaryExprNode;
import ast.statements.AssignmentNode;
import ast.statements.BlockNode;
import ast.statements.CallStatementNode;
import ast.statements.DeclarationNode;
import ast.statements.FunctionDeclNode;
import ast.statements.IfNode;
import ast.statements.ParameterNode;
import ast.statements.PrintNode;
import ast.statements.ReturnNode;
import ast.statements.StatementNode;
import ast.statements.WhileNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import types.ValueType;

/**
 * Valida el AST y calcula la informacion semantica del programa.
 */
public class SemanticAnalyzer {
    private static final ValueType ERROR_TYPE = ValueType.ERROR;

    private SymbolTable globalScope = new SymbolTable(null);
    private SymbolTable currentScope = globalScope;
    private final Map<String, FunctionSymbol> functions = new HashMap<>();
    private final Map<String, Symbol> globalSymbols = new HashMap<>();
    private final IdentityHashMap<DeclarationNode, Symbol> declarationSymbols = new IdentityHashMap<>();
    private final IdentityHashMap<AssignmentNode, ValueType> assignmentTargetTypes = new IdentityHashMap<>();
    private final IdentityHashMap<ExpressionNode, ValueType> expressionTypes = new IdentityHashMap<>();
    private final List<SemanticException> errors = new ArrayList<>();
    private FunctionDeclNode currentFunction;
    private Symbol symbolBeingInitialized;

    // Devuelve todos los errores semanticos registrados durante el analisis.
    public List<SemanticException> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    // Indica si el analizador detecto algun error recuperable.
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // Reinicia el estado y lanza todas las fases del analisis semantico.
    public SemanticInfo analyze(ProgramNode program) {
        globalScope = new SymbolTable(null);
        currentScope = globalScope;
        functions.clear();
        globalSymbols.clear();
        declarationSymbols.clear();
        assignmentTargetTypes.clear();
        expressionTypes.clear();
        errors.clear();
        currentFunction = null;
        symbolBeingInitialized = null;

        registerFunctions(program);
        registerGlobalVariables(program);
        analyzeGlobalInitializers(program);
        analyzeFunctions(program);

        return new SemanticInfo(functions, globalSymbols, assignmentTargetTypes, expressionTypes);
    }

    // Registra primero las funciones para permitir llamadas adelantadas.
    private void registerFunctions(ProgramNode program) {
        for (GlobalElement element : program.getGlobalElements()) {
            if (!(element instanceof FunctionDeclNode function)) {
                continue;
            }

            if (functions.containsKey(function.getName())) {
                report(function, "La funcion ya esta declarada: " + function.getName());
                continue;
            }

            List<ValueType> parameterTypes = new ArrayList<>();
            for (ParameterNode parameter : function.getParameters()) {
                parameterTypes.add(parameter.getType());
            }

            functions.put(function.getName(), new FunctionSymbol(
                    function.getName(),
                    function.getReturnType(),
                    parameterTypes
            ));
        }
    }

    // Registra las variables globales antes de validar sus inicializadores.
    private void registerGlobalVariables(ProgramNode program) {
        for (GlobalElement element : program.getGlobalElements()) {
            if (!(element instanceof DeclarationNode declaration)) {
                continue;
            }

            ensureVariableNameAvailable(globalScope, declaration, declaration.getIdentifier());
            if (hasNameConflict(globalScope, declaration.getIdentifier())) {
                continue;
            }

            Symbol symbol = new Symbol(
                    declaration.getIdentifier(),
                    declaration.getType(),
                    declaration.isConstant()
            );
            globalScope.define(symbol);
            globalSymbols.put(symbol.getName(), symbol);
            declarationSymbols.put(declaration, symbol);
        }
    }

    // Valida las expresiones iniciales de las variables globales.
    private void analyzeGlobalInitializers(ProgramNode program) {
        currentScope = globalScope;

        for (GlobalElement element : program.getGlobalElements()) {
            if (element instanceof DeclarationNode declaration) {
                Symbol symbol = declarationSymbols.get(declaration);
                analyzeDeclarationInitializer(declaration, symbol);
            }
        }
    }

    // Analiza semanticamente cada funcion declarada en el programa.
    private void analyzeFunctions(ProgramNode program) {
        for (GlobalElement element : program.getGlobalElements()) {
            if (element instanceof FunctionDeclNode function) {
                analyzeFunction(function);
            }
        }
    }

    // Crea el ambito de la funcion y valida sus parametros, cuerpo y retorno.
    private void analyzeFunction(FunctionDeclNode function) {
        SymbolTable previousScope = currentScope;
        FunctionDeclNode previousFunction = currentFunction;

        currentScope = new SymbolTable(globalScope);
        currentFunction = function;

        try {
            for (ParameterNode parameter : function.getParameters()) {
                if (currentScope.isDefinedLocally(parameter.getIdentifier())) {
                    report(parameter, "Parametro duplicado: " + parameter.getIdentifier());
                    continue;
                }

                currentScope.define(new Symbol(
                        parameter.getIdentifier(),
                        parameter.getType(),
                        false
                ));
            }

            analyzeBlockContents(function.getBody());

            if (!function.getReturnType().isVoid() && !alwaysReturns(function.getBody())) {
                report(function, "La funcion '" + function.getName()
                        + "' debe devolver un valor de tipo " + function.getReturnType());
            }
        } finally {
            currentScope = previousScope;
            currentFunction = previousFunction;
        }
    }

    // Despacha el analisis segun el tipo de nodo recibido.
    private void analyzeNode(ASTNode node) {
        if (node instanceof DeclarationNode declaration) {
            analyzeLocalDeclaration(declaration);
            return;
        }

        if (node instanceof AssignmentNode assignment) {
            Symbol symbol = resolveVariable(assignment.getIdentifier(), assignment);
            if (symbol != null && symbol.isConstant()) {
                report(assignment, "No se puede modificar la constante '" + assignment.getIdentifier() + "'");
            }

            ValueType expressionType = analyzeExpression(assignment.getExpression());
            if (symbol != null) {
                assignmentTargetTypes.put(assignment, symbol.getType());
                ensureAssignable(symbol.getType(), expressionType, assignment,
                        "No se puede asignar un valor de tipo " + expressionType + " a '"
                                + assignment.getIdentifier() + "' de tipo " + symbol.getType());
            }
            return;
        }

        if (node instanceof BlockNode block) {
            analyzeBlock(block);
            return;
        }

        if (node instanceof CallStatementNode callStatementNode) {
            analyzeCallStatement(callStatementNode);
            return;
        }

        if (node instanceof IfNode ifNode) {
            ValueType conditionType = analyzeExpression(ifNode.getCondition());
            ensureType(ValueType.JURA, conditionType, ifNode.getCondition(),
                    "La condicion de 'fijateSi' debe ser de tipo jura");
            analyzeBlock(ifNode.getThenBlock());

            if (ifNode.getElseBlock() != null) {
                analyzeBlock(ifNode.getElseBlock());
            }
            return;
        }

        if (node instanceof WhileNode whileNode) {
            ValueType conditionType = analyzeExpression(whileNode.getCondition());
            ensureType(ValueType.JURA, conditionType, whileNode.getCondition(),
                    "La condicion de 'segunVea' debe ser de tipo jura");
            analyzeBlock(whileNode.getBody());
            return;
        }

        if (node instanceof PrintNode printNode) {
            for (ExpressionNode argument : printNode.getArguments()) {
                ValueType argumentType = analyzeExpression(argument);
                if (argumentType.isVoid()) {
                    report(argument, "No se puede usar un valor de tipo naDeNa en 'di!'");
                }
            }
            return;
        }

        if (node instanceof ReturnNode returnNode) {
            analyzeReturn(returnNode);
            return;
        }

        if (node instanceof FunctionDeclNode functionNode) {
            analyzeFunction(functionNode);
        }
    }

    // Declara una variable local y comprueba su inicializador si existe.
    private void analyzeLocalDeclaration(DeclarationNode declaration) {
        ensureVariableNameAvailable(currentScope, declaration, declaration.getIdentifier());
        if (hasNameConflict(currentScope, declaration.getIdentifier())) {
            analyzeDeclarationInitializer(declaration, null);
            return;
        }

        Symbol symbol = new Symbol(
                declaration.getIdentifier(),
                declaration.getType(),
                declaration.isConstant()
        );
        currentScope.define(symbol);
        declarationSymbols.put(declaration, symbol);
        analyzeDeclarationInitializer(declaration, symbol);
    }

    // Valida que el inicializador encaje con el tipo declarado.
    private void analyzeDeclarationInitializer(DeclarationNode declaration, Symbol symbol) {
        if (declaration.getInitializer() == null) {
            return;
        }

        if (symbol == null) {
            analyzeExpression(declaration.getInitializer());
            return;
        }

        Symbol previousInitializing = symbolBeingInitialized;
        symbolBeingInitialized = symbol;

        try {
            ValueType initializerType = analyzeExpression(declaration.getInitializer());
            ensureAssignable(symbol.getType(), initializerType, declaration.getInitializer(),
                    "No se puede inicializar '" + declaration.getIdentifier() + "' de tipo " + symbol.getType()
                            + " con un valor de tipo " + initializerType);
        } finally {
            symbolBeingInitialized = previousInitializing;
        }
    }

    // Crea un ambito nuevo para un bloque de sentencias.
    private void analyzeBlock(BlockNode block) {
        SymbolTable previousScope = currentScope;
        currentScope = new SymbolTable(previousScope);

        try {
            analyzeBlockContents(block);
        } finally {
            currentScope = previousScope;
        }
    }

    // Analiza las sentencias de un bloque sin crear otro ambito.
    private void analyzeBlockContents(BlockNode block) {
        for (StatementNode statement : block.getStatements()) {
            analyzeNode(statement);
        }
    }

    // Comprueba que 'suelta' sea valida para la funcion actual.
    private void analyzeReturn(ReturnNode returnNode) {
        if (currentFunction == null) {
            report(returnNode, "'suelta' solo puede aparecer dentro de una funcion");
            if (returnNode.getExpression() != null) {
                analyzeExpression(returnNode.getExpression());
            }
            return;
        }

        ValueType expectedType = currentFunction.getReturnType();
        ExpressionNode returnedExpression = returnNode.getExpression();

        if (expectedType.isVoid()) {
            if (returnedExpression != null) {
                report(returnedExpression, "La funcion '" + currentFunction.getName()
                        + "' devuelve naDeNa y no puede devolver un valor");
                analyzeExpression(returnedExpression);
            }
            return;
        }

        if (returnedExpression == null) {
            report(returnNode, "La funcion '" + currentFunction.getName()
                    + "' debe devolver un valor de tipo " + expectedType);
            return;
        }

        ValueType actualType = analyzeExpression(returnedExpression);
        ensureAssignable(expectedType, actualType, returnedExpression,
                "La funcion '" + currentFunction.getName() + "' debe devolver "
                        + expectedType + ", pero se encontro " + actualType);
    }

    // Calcula y valida el tipo de una expresion.
    private ValueType analyzeExpression(ExpressionNode expression) {
        if (expression instanceof IdentifierExprNode identifier) {
            Symbol symbol = resolveVariable(identifier.getName(), identifier);
            if (symbol == null) {
                return rememberExpressionType(expression, ERROR_TYPE);
            }
            if (symbol == symbolBeingInitialized) {
                report(identifier, "La variable '" + identifier.getName()
                        + "' no puede usarse en su propio inicializador");
                return rememberExpressionType(expression, ERROR_TYPE);
            }
            return rememberExpressionType(expression, symbol.getType());
        }

        if (expression instanceof LiteralExprNode literal) {
            return rememberExpressionType(expression, typeOfLiteral(literal));
        }

        if (expression instanceof UnaryExprNode unary) {
            ValueType rightType = analyzeExpression(unary.getRight());
            if (isErrorType(rightType)) {
                return rememberExpressionType(expression, ERROR_TYPE);
            }
            if (!rightType.isNumeric()) {
                report(unary, "El operador '" + unary.getOperator()
                        + "' necesita un operando numerico");
                return rememberExpressionType(expression, ERROR_TYPE);
            }
            return rememberExpressionType(expression, rightType);
        }

        if (expression instanceof BinaryExprNode binary) {
            ValueType leftType = analyzeExpression(binary.getLeft());
            ValueType rightType = analyzeExpression(binary.getRight());
            if (isErrorType(leftType) || isErrorType(rightType)) {
                return rememberExpressionType(expression, ERROR_TYPE);
            }

            ValueType resultType = switch (binary.getOperator()) {
                case "+", "-", "*", "/" -> analyzeArithmetic(binary, leftType, rightType);
                case "<", ">", "<=", ">=" -> analyzeRelational(binary, leftType, rightType);
                case "==" -> analyzeEquality(binary, leftType, rightType);
                case "y", "o" -> analyzeLogical(binary, leftType, rightType);
                default -> throw new IllegalStateException("Operador desconocido: " + binary.getOperator());
            };
            return rememberExpressionType(expression, resultType);
        }

        if (expression instanceof CallExprNode call) {
            return analyzeCall(call);
        }

        throw new IllegalStateException("Nodo de expresion no soportado: "
                + expression.getClass().getSimpleName());
    }

    // Conserva el tipo calculado para que fases posteriores puedan reutilizarlo.
    private ValueType rememberExpressionType(ExpressionNode expression, ValueType type) {
        expressionTypes.put(expression, type);
        return type;
    }

    // Valida una llamada usada como sentencia y exige retorno naDeNa.
    private void analyzeCallStatement(CallStatementNode callStatementNode) {
        CallExprNode call = callStatementNode.getCall();
        ValueType returnType = analyzeCall(call);
        if (isErrorType(returnType)) {
            return;
        }

        if (!returnType.isVoid()) {
            report(callStatementNode, "La llamada a '" + call.getCallee()
                    + "' devuelve " + returnType + " y no puede usarse como sentencia");
        }
    }

    // Valida una llamada a funcion y devuelve su tipo de retorno.
    private ValueType analyzeCall(CallExprNode call) {
        FunctionSymbol function = functions.get(call.getCallee());
        if (function == null) {
            report(call, "Funcion no declarada: " + call.getCallee());
            for (ExpressionNode argument : call.getArguments()) {
                analyzeExpression(argument);
            }
            return rememberExpressionType(call, ERROR_TYPE);
        }

        if (call.getArguments().size() != function.getParameterTypes().size()) {
            report(call, "La funcion '" + call.getCallee() + "' espera "
                    + function.getParameterTypes().size() + " argumentos, pero recibio "
                    + call.getArguments().size());
        }

        for (int i = 0; i < call.getArguments().size(); i++) {
            ExpressionNode argument = call.getArguments().get(i);
            ValueType argumentType = analyzeExpression(argument);
            if (i >= function.getParameterTypes().size()) {
                continue;
            }

            ValueType parameterType = function.getParameterTypes().get(i);
            ensureAssignable(parameterType, argumentType, argument,
                    "El argumento " + (i + 1) + " de la funcion '" + call.getCallee()
                            + "' debe ser " + parameterType + ", pero se encontro " + argumentType);
        }

        return rememberExpressionType(call, function.getReturnType());
    }

    // Valida una operacion aritmetica y devuelve el tipo resultante.
    private ValueType analyzeArithmetic(ASTNode node, ValueType leftType, ValueType rightType) {
        if (!leftType.isNumeric() || !rightType.isNumeric()) {
            report(node, "Los operadores aritmeticos necesitan operandos numericos, pero se encontraron "
                    + leftType + " y " + rightType);
            return ERROR_TYPE;
        }

        return ValueType.promoteNumeric(leftType, rightType);
    }

    // Valida una comparacion relacional y devuelve jura.
    private ValueType analyzeRelational(ASTNode node, ValueType leftType, ValueType rightType) {
        if (!leftType.isNumeric() || !rightType.isNumeric()) {
            report(node, "Los operadores de comparacion necesitan operandos numericos, pero se encontraron "
                    + leftType + " y " + rightType);
            return ERROR_TYPE;
        }

        return ValueType.JURA;
    }

    // Valida que ambos lados de == se puedan comparar.
    private ValueType analyzeEquality(ASTNode node, ValueType leftType, ValueType rightType) {
        if (!leftType.isComparableWith(rightType)) {
            report(node, "El operador '==' no puede comparar valores de tipo "
                    + leftType + " y " + rightType);
            return ERROR_TYPE;
        }

        return ValueType.JURA;
    }

    // Valida operadores logicos sobre valores booleanos del lenguaje.
    private ValueType analyzeLogical(ASTNode node, ValueType leftType, ValueType rightType) {
        if (leftType != ValueType.JURA || rightType != ValueType.JURA) {
            report(node, "Los operadores logicos necesitan operandos de tipo jura, pero se encontraron "
                    + leftType + " y " + rightType);
            return ERROR_TYPE;
        }

        return ValueType.JURA;
    }

    // Comprueba que un nombre de variable pueda declararse en el ambito dado.
    private void ensureVariableNameAvailable(SymbolTable scope, ASTNode node, String name) {
        if (scope.isDefinedLocally(name)) {
            report(node, "La variable ya esta declarada en este ambito: " + name);
        }

        if (scope == globalScope && functions.containsKey(name)) {
            report(node, "El nombre '" + name + "' ya lo usa una funcion");
        }
    }

    // Busca una variable en el ambito actual y sus padres.
    private Symbol resolveVariable(String name, ASTNode node) {
        Symbol symbol = currentScope.resolve(name);
        if (symbol == null) {
            report(node, "Variable no declarada: " + name);
        }
        return symbol;
    }

    // Lanza error si un tipo no puede asignarse a otro.
    private void ensureAssignable(ValueType expectedType, ValueType actualType, ASTNode node, String message) {
        if (isErrorType(actualType)) {
            return;
        }

        if (!expectedType.accepts(actualType)) {
            report(node, message);
        }
    }

    // Comprueba que el tipo real coincida con el esperado.
    private void ensureType(ValueType expectedType, ValueType actualType, ASTNode node, String message) {
        if (isErrorType(actualType)) {
            return;
        }

        if (expectedType != actualType) {
            report(node, message + " (se encontro " + actualType + ")");
        }
    }

    // Detecta el tipo sentinel usado para cortar cascadas de errores.
    private boolean isErrorType(ValueType type) {
        return type == null || type.isError();
    }

    // Traduce cada token literal al tipo semantico correspondiente.
    private ValueType typeOfLiteral(LiteralExprNode literal) {
        return switch (literal.getLiteralType()) {
            case INT_LITERAL -> ValueType.PAVOS;
            case FLOAT_LITERAL -> ValueType.APACHAS;
            case CHAR_LITERAL -> ValueType.LETRILLA;
            case STRING_LITERAL -> ValueType.CARRO;
            case JURAO, BULO -> ValueType.JURA;
            default -> throw new IllegalStateException("Token literal no soportado: " + literal.getLiteralType());
        };
    }

    // Comprueba si un bloque garantiza algun retorno.
    private boolean alwaysReturns(BlockNode block) {
        for (StatementNode statement : block.getStatements()) {
            if (alwaysReturns(statement)) {
                return true;
            }
        }
        return false;
    }

    // Comprueba si una sentencia garantiza retorno por todos sus caminos.
    private boolean alwaysReturns(StatementNode statement) {
        if (statement instanceof ReturnNode) {
            return true;
        }

        if (statement instanceof BlockNode block) {
            return alwaysReturns(block);
        }

        if (statement instanceof IfNode ifNode) {
            return ifNode.getElseBlock() != null
                    && alwaysReturns(ifNode.getThenBlock())
                    && alwaysReturns(ifNode.getElseBlock());
        }

        return false;
    }

    // Comprueba si un nombre choca con otro ya definido en el ambito.
    private boolean hasNameConflict(SymbolTable scope, String name) {
        return scope.isDefinedLocally(name) || (scope == globalScope && functions.containsKey(name));
    }

    // Registra un error semantico recuperable.
    private void report(ASTNode node, String message) {
        errors.add(new SemanticException(node, message));
    }
}
