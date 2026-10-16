package ir;

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
import semantic.SemanticInfo;
import types.ValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduce el AST validado a codigo intermedio.
 */
public class IRGenerator {
    private final SemanticInfo semanticInfo;
    private IRNameGenerator nameGenerator;
    private IRProgram currentProgram;
    private IRFunction currentFunction;
    private final List<Map<String, IRBinding>> variableScopes = new ArrayList<>();
    private final Map<String, String> usedLocalNames = new HashMap<>();
    private int nextScopedVariableId;

    public IRGenerator(SemanticInfo semanticInfo) {
        this.semanticInfo = semanticInfo;
    }

    // Traduce la estructura de alto nivel del AST a un programa IR.
    public IRProgram generate(ProgramNode program) {
        nameGenerator = new IRNameGenerator();
        currentProgram = new IRProgram();
        variableScopes.clear();
        usedLocalNames.clear();
        nextScopedVariableId = 0;
        pushScope();

        try {
            for (GlobalElement element : program.getGlobalElements()) {
                if (element instanceof DeclarationNode declaration) {
                    defineVariable(declaration.getIdentifier(), declaration.getIdentifier(), declaration.getType());
                    currentProgram.addGlobalVariable(new IRVariable(declaration.getType(), declaration.getIdentifier()));
                }
            }

            for (GlobalElement element : program.getGlobalElements()) {
                if (element instanceof DeclarationNode declaration) {
                    generateDeclaration(declaration);
                }
            }

            for (GlobalElement element : program.getGlobalElements()) {
                if (element instanceof FunctionDeclNode function) {
                    currentProgram.addFunction(generateFunction(function));
                }
            }

            return currentProgram;
        } finally {
            variableScopes.clear();
            usedLocalNames.clear();
            currentProgram = null;
        }
    }

    // Traduce una funcion y las sentencias soportadas de su cuerpo.
    private IRFunction generateFunction(FunctionDeclNode function) {
        IRFunction previousFunction = currentFunction;
        currentFunction = new IRFunction(
                function.getName(),
                function.getReturnType(),
                generateParameters(function.getParameters())
        );
        usedLocalNames.clear();
        pushScope();

        try {
            for (IRParameter parameter : currentFunction.getParameters()) {
                defineVariable(parameter.getName(), parameter.getName(), parameter.getType());
                usedLocalNames.put(parameter.getName(), parameter.getName());
            }

            generateBlockContents(function.getBody());
            return currentFunction;
        } finally {
            popScope();
            usedLocalNames.clear();
            currentFunction = previousFunction;
        }
    }

    private void generateBlock(BlockNode block) {
        pushScope();
        try {
            generateBlockContents(block);
        } finally {
            popScope();
        }
    }

    private void generateBlockContents(BlockNode block) {
        for (StatementNode statement : block.getStatements()) {
            generateStatement(statement);
        }
    }

    private void generateStatement(StatementNode statement) {
        if (statement instanceof DeclarationNode declaration) {
            generateDeclaration(declaration);
            return;
        }

        if (statement instanceof AssignmentNode assignment) {
            generateAssignment(assignment);
            return;
        }

        if (statement instanceof CallStatementNode callStatementNode) {
            generateCallStatement(callStatementNode);
            return;
        }

        if (statement instanceof ReturnNode returnNode) {
            generateReturn(returnNode);
            return;
        }

        if (statement instanceof PrintNode printNode) {
            generatePrint(printNode);
            return;
        }

        if (statement instanceof IfNode ifNode) {
            generateIf(ifNode);
            return;
        }

        if (statement instanceof WhileNode whileNode) {
            generateWhile(whileNode);
            return;
        }

        throw new IRGenerationException(statement, "Sentencia no soportada en IR: "
                + statement.getClass().getSimpleName());
    }

    private void generateDeclaration(DeclarationNode declaration) {
        String irName = declaration.getIdentifier();
        if (currentFunction != null) {
            irName = allocateLocalName(declaration.getIdentifier());
            defineVariable(declaration.getIdentifier(), irName, declaration.getType());
            currentFunction.addLocalVariable(new IRVariable(
                    declaration.getType(),
                    irName
            ));
        } else if (resolveVariable(declaration.getIdentifier()) == null) {
            defineVariable(declaration.getIdentifier(), irName, declaration.getType());
        }

        if (declaration.getInitializer() == null) {
            return;
        }

        IROperand value = generateExpression(declaration.getInitializer());
        emit(new AssignInstruction(
                new VariableOperand(irName, declaration.getType()),
                value
        ));
    }

    private void generateAssignment(AssignmentNode assignment) {
        ValueType targetType = semanticInfo.getAssignmentTargetType(assignment);
        if (targetType == null) {
            throw new IRGenerationException(assignment,
                    "No se encontro tipo semantico para la asignacion a '" + assignment.getIdentifier() + "'");
        }

        IROperand value = generateExpression(assignment.getExpression());
        IRBinding binding = requireVariable(assignment.getIdentifier(), assignment);
        emit(new AssignInstruction(
                new VariableOperand(binding.irName(), targetType),
                value
        ));
    }

    private void generateReturn(ReturnNode returnNode) {
        IROperand value = null;
        if (returnNode.getExpression() != null) {
            value = generateExpression(returnNode.getExpression());
        }

        emit(new ReturnInstruction(value));
    }

    private void generateCallStatement(CallStatementNode callStatementNode) {
        CallExprNode call = callStatementNode.getCall();
        List<IROperand> arguments = new ArrayList<>();

        for (ExpressionNode argument : call.getArguments()) {
            arguments.add(generateExpression(argument));
        }

        emit(new CallInstruction(null, call.getCallee(), arguments));
    }

    private void generatePrint(PrintNode printNode) {
        List<IROperand> arguments = new ArrayList<>();

        for (ExpressionNode argument : printNode.getArguments()) {
            arguments.add(generateExpression(argument));
        }

        emit(new PrintInstruction(arguments));
    }

    private void generateIf(IfNode ifNode) {
        IROperand condition = generateExpression(ifNode.getCondition());

        if (ifNode.getElseBlock() == null) {
            LabelOperand endLabel = nameGenerator.newLabel();

            emit(new IfFalseGotoInstruction(condition, endLabel));
            generateBlock(ifNode.getThenBlock());
            emit(new LabelInstruction(endLabel));
            return;
        }

        LabelOperand elseLabel = nameGenerator.newLabel();
        LabelOperand endLabel = nameGenerator.newLabel();

        emit(new IfFalseGotoInstruction(condition, elseLabel));
        generateBlock(ifNode.getThenBlock());
        emit(new GotoInstruction(endLabel));
        emit(new LabelInstruction(elseLabel));
        generateBlock(ifNode.getElseBlock());
        emit(new LabelInstruction(endLabel));
    }

    private void generateWhile(WhileNode whileNode) {
        LabelOperand startLabel = nameGenerator.newLabel();
        LabelOperand endLabel = nameGenerator.newLabel();

        emit(new LabelInstruction(startLabel));
        IROperand condition = generateExpression(whileNode.getCondition());
        emit(new IfFalseGotoInstruction(condition, endLabel));
        generateBlock(whileNode.getBody());
        emit(new GotoInstruction(startLabel));
        emit(new LabelInstruction(endLabel));
    }

    private IROperand generateExpression(ExpressionNode expression) {
        if (expression instanceof LiteralExprNode literal) {
            return new LiteralOperand(formatLiteral(literal), semanticInfo.getExpressionType(expression));
        }

        if (expression instanceof IdentifierExprNode identifier) {
            IRBinding binding = requireVariable(identifier.getName(), expression);
            return new VariableOperand(binding.irName(), semanticInfo.getExpressionType(expression));
        }

        if (expression instanceof UnaryExprNode unary) {
            IROperand value = generateExpression(unary.getRight());
            TempOperand target = nameGenerator.newTemp(semanticInfo.getExpressionType(expression));
            emit(new UnaryInstruction(target, unary.getOperator(), value));
            return target;
        }

        if (expression instanceof BinaryExprNode binary) {
            if ("y".equals(binary.getOperator())) {
                return generateShortCircuitAnd(binary);
            }

            if ("o".equals(binary.getOperator())) {
                return generateShortCircuitOr(binary);
            }

            IROperand left = generateExpression(binary.getLeft());
            IROperand right = generateExpression(binary.getRight());
            TempOperand target = nameGenerator.newTemp(semanticInfo.getExpressionType(expression));
            emit(new BinaryInstruction(target, left, binary.getOperator(), right));
            return target;
        }

        if (expression instanceof CallExprNode call) {
            return generateCall(call);
        }

        throw new IRGenerationException(expression, "Expresion no soportada en IR: "
                + expression.getClass().getSimpleName());
    }

    private IROperand generateCall(CallExprNode call) {
        ValueType returnType = semanticInfo.getExpressionType(call);
        if (returnType == null) {
            throw new IRGenerationException(call, "No se encontro tipo semantico para la llamada a '"
                    + call.getCallee() + "'");
        }

        if (returnType.isVoid()) {
            throw new IRGenerationException(call, "La llamada a '" + call.getCallee()
                    + "' no produce un valor usable como expresion");
        }

        List<IROperand> arguments = new ArrayList<>();
        for (ExpressionNode argument : call.getArguments()) {
            arguments.add(generateExpression(argument));
        }

        TempOperand target = nameGenerator.newTemp(returnType);
        emit(new CallInstruction(target, call.getCallee(), arguments));
        return target;
    }

    private void emit(IRInstruction instruction) {
        if (currentFunction != null) {
            currentFunction.addInstruction(instruction);
            return;
        }

        currentProgram.addGlobalInstruction(instruction);
    }

    private String formatLiteral(LiteralExprNode literal) {
        return switch (literal.getLiteralType()) {
            case STRING_LITERAL -> "\"" + escapeLiteral(literal.getValue()) + "\"";
            case CHAR_LITERAL -> "'" + escapeLiteral(literal.getValue()) + "'";
            default -> literal.getValue();
        };
    }

    private String escapeLiteral(String value) {
        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char currentChar = value.charAt(i);
            switch (currentChar) {
                case '\n' -> escaped.append("\\n");
                case '\t' -> escaped.append("\\t");
                case '\r' -> escaped.append("\\r");
                case '"' -> escaped.append("\\\"");
                case '\'' -> escaped.append("\\'");
                case '\\' -> escaped.append("\\\\");
                default -> escaped.append(currentChar);
            }
        }

        return escaped.toString();
    }

    private List<IRParameter> generateParameters(List<ParameterNode> parameters) {
        List<IRParameter> irParameters = new ArrayList<>();

        for (ParameterNode parameter : parameters) {
            irParameters.add(new IRParameter(parameter.getType(), parameter.getIdentifier()));
        }

        return irParameters;
    }

    private IROperand generateShortCircuitAnd(BinaryExprNode binary) {
        TempOperand target = nameGenerator.newTemp(semanticInfo.getExpressionType(binary));
        LabelOperand falseLabel = nameGenerator.newLabel();
        LabelOperand endLabel = nameGenerator.newLabel();

        IROperand left = generateExpression(binary.getLeft());
        emit(new IfFalseGotoInstruction(left, falseLabel));

        IROperand right = generateExpression(binary.getRight());
        emit(new IfFalseGotoInstruction(right, falseLabel));

        emit(new AssignInstruction(target, new LiteralOperand("jurao", ValueType.JURA)));
        emit(new GotoInstruction(endLabel));
        emit(new LabelInstruction(falseLabel));
        emit(new AssignInstruction(target, new LiteralOperand("bulo", ValueType.JURA)));
        emit(new LabelInstruction(endLabel));
        return target;
    }

    private IROperand generateShortCircuitOr(BinaryExprNode binary) {
        TempOperand target = nameGenerator.newTemp(semanticInfo.getExpressionType(binary));
        LabelOperand evalRightLabel = nameGenerator.newLabel();
        LabelOperand falseLabel = nameGenerator.newLabel();
        LabelOperand endLabel = nameGenerator.newLabel();

        IROperand left = generateExpression(binary.getLeft());
        emit(new IfFalseGotoInstruction(left, evalRightLabel));

        emit(new AssignInstruction(target, new LiteralOperand("jurao", ValueType.JURA)));
        emit(new GotoInstruction(endLabel));
        emit(new LabelInstruction(evalRightLabel));

        IROperand right = generateExpression(binary.getRight());
        emit(new IfFalseGotoInstruction(right, falseLabel));

        emit(new AssignInstruction(target, new LiteralOperand("jurao", ValueType.JURA)));
        emit(new GotoInstruction(endLabel));
        emit(new LabelInstruction(falseLabel));
        emit(new AssignInstruction(target, new LiteralOperand("bulo", ValueType.JURA)));
        emit(new LabelInstruction(endLabel));
        return target;
    }

    private void pushScope() {
        variableScopes.add(new HashMap<>());
    }

    private void popScope() {
        variableScopes.remove(variableScopes.size() - 1);
    }

    private void defineVariable(String sourceName, String irName, ValueType type) {
        variableScopes.get(variableScopes.size() - 1).put(sourceName, new IRBinding(irName, type));
    }

    private IRBinding resolveVariable(String sourceName) {
        for (int i = variableScopes.size() - 1; i >= 0; i--) {
            IRBinding binding = variableScopes.get(i).get(sourceName);
            if (binding != null) {
                return binding;
            }
        }

        return null;
    }

    private IRBinding requireVariable(String sourceName, ExpressionNode node) {
        IRBinding binding = resolveVariable(sourceName);
        if (binding == null) {
            throw new IRGenerationException(node, "No se encontro variable IR para '" + sourceName + "'");
        }

        return binding;
    }

    private IRBinding requireVariable(String sourceName, AssignmentNode node) {
        IRBinding binding = resolveVariable(sourceName);
        if (binding == null) {
            throw new IRGenerationException(node, "No se encontro variable IR para '" + sourceName + "'");
        }

        return binding;
    }

    private String allocateLocalName(String sourceName) {
        if (!usedLocalNames.containsKey(sourceName) && resolveVariable(sourceName) == null) {
            usedLocalNames.put(sourceName, sourceName);
            return sourceName;
        }

        String candidate;
        do {
            candidate = sourceName + "__" + nextScopedVariableId++;
        } while (usedLocalNames.containsKey(candidate));

        usedLocalNames.put(candidate, candidate);
        return candidate;
    }

    private record IRBinding(String irName, ValueType type) {
    }
}
