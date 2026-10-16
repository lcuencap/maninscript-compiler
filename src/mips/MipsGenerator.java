package mips;

import ir.AssignInstruction;
import ir.BinaryInstruction;
import ir.CallInstruction;
import ir.GotoInstruction;
import ir.IRFunction;
import ir.IRInstruction;
import ir.IROperand;
import ir.IRParameter;
import ir.IRProgram;
import ir.IRVariable;
import ir.IfFalseGotoInstruction;
import ir.LabelInstruction;
import ir.LiteralOperand;
import ir.PrintInstruction;
import ir.ReturnInstruction;
import ir.TempOperand;
import ir.UnaryInstruction;
import ir.VariableOperand;
import types.ValueType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera codigo MIPS a partir del programa IR/TAC.
 */
public class MipsGenerator {
    private static final String DATA_SECTION = ".data";
    private static final String TEXT_SECTION = ".text";
    private static final String ENTRY_POINT_LABEL = "__start";
    private static final String GLOBAL_INIT_LABEL = "__init_globals";
    private static final String STRING_EQUALS_LABEL = "__string_equals";
    private static final int WORD_SIZE = 4;
    private static final int SAVED_FRAME_BYTES = 8;

    private final Map<String, String> stringLabels = new LinkedHashMap<>();
    private final Map<String, String> floatLabels = new LinkedHashMap<>();
    private final Map<String, String> globalLabels = new LinkedHashMap<>();
    private final Map<String, ValueType> globalTypes = new LinkedHashMap<>();
    private final Map<String, IRFunction> functionsByName = new LinkedHashMap<>();
    private final Map<String, Integer> stackOffsets = new LinkedHashMap<>();
    private final Map<String, ValueType> localTypes = new LinkedHashMap<>();

    private IRProgram currentProgram;
    private IRFunction currentFunction;
    private boolean insideGlobalInit;
    private String currentExitLabel;
    private StringBuilder dataSection = new StringBuilder();
    private StringBuilder textSection = new StringBuilder();
    private int nextStringId;
    private int nextFloatId;
    private int nextInternalLabelId;
    private int currentLocalStackSize;

    // Traduce el programa IR completo a ensamblador MIPS.
    public String generate(IRProgram program) {
        resetState();
        currentProgram = program;

        try {
            indexProgramMetadata();
            generateDataSection();
            generateTextSection();
            return dataSection.toString() + textSection;
        } finally {
            currentProgram = null;
            currentFunction = null;
            currentExitLabel = null;
            insideGlobalInit = false;
        }
    }

    // Reinicia el estado interno para una nueva generacion.
    private void resetState() {
        stringLabels.clear();
        floatLabels.clear();
        globalLabels.clear();
        globalTypes.clear();
        functionsByName.clear();
        stackOffsets.clear();
        localTypes.clear();
        dataSection = new StringBuilder();
        textSection = new StringBuilder();
        nextStringId = 0;
        nextFloatId = 0;
        nextInternalLabelId = 0;
        currentLocalStackSize = 0;
        currentExitLabel = null;
        insideGlobalInit = false;
    }

    // Registra metadatos globales antes de emitir codigo.
    private void indexProgramMetadata() {
        if (currentProgram == null) {
            return;
        }

        for (IRVariable globalVariable : currentProgram.getGlobalVariables()) {
            globalLabels.put(globalVariable.getName(), "global_" + globalVariable.getName());
            globalTypes.put(globalVariable.getName(), globalVariable.getType());
        }

        for (IRFunction function : currentProgram.getFunctions()) {
            functionsByName.put(function.getName(), function);
        }
    }

    // Genera las declaraciones de la seccion de datos.
    private void generateDataSection() {
        emitDataLine(DATA_SECTION);

        if (currentProgram == null) {
            return;
        }

        for (IRVariable globalVariable : currentProgram.getGlobalVariables()) {
            declareGlobalStorage(globalVariable);
        }
    }

    // Genera la seccion de texto con el arranque y las funciones.
    private void generateTextSection() {
        if (currentProgram == null) {
            return;
        }

        emitTextLine("");
        emitTextLine(TEXT_SECTION);
        emitEntryPoint();
        emitGlobalInitRoutine();

        for (IRFunction function : currentProgram.getFunctions()) {
            generateFunction(function);
        }

        emitStringEqualsHelper();
    }

    // Emite el punto de entrada ejecutable del fichero ASM.
    private void emitEntryPoint() {
        emitTextLine("");
        emitTextLine(ENTRY_POINT_LABEL + ":");

        if (!currentProgram.getGlobalInstructions().isEmpty()) {
            emitTextLine("jal " + GLOBAL_INIT_LABEL);
        }

        IRFunction entryFunction = selectEntryFunction();
        if (entryFunction != null) {
            emitTextLine("jal " + entryFunction.getName());
        }

        emitTextLine("li $v0, 10");
        emitTextLine("syscall");
    }

    // Elige una funcion razonable para lanzar automaticamente al arrancar.
    private IRFunction selectEntryFunction() {
        IRFunction mainFunction = functionsByName.get("main");
        if (mainFunction != null && mainFunction.getParameters().isEmpty()) {
            return mainFunction;
        }

        for (IRFunction function : currentProgram.getFunctions()) {
            if (function.getParameters().isEmpty()) {
                return function;
            }
        }

        return null;
    }

    // Emite una rutina separada para evaluar los inicializadores globales.
    private void emitGlobalInitRoutine() {
        emitTextLine("");
        emitTextLine(GLOBAL_INIT_LABEL + ":");

        if (currentProgram.getGlobalInstructions().isEmpty()) {
            emitTextLine("jr $ra");
            return;
        }

        IRFunction previousFunction = currentFunction;
        boolean previousInsideGlobalInit = insideGlobalInit;
        String previousExitLabel = currentExitLabel;

        currentFunction = null;
        insideGlobalInit = true;
        currentExitLabel = null;
        prepareGlobalInitFrame(currentProgram.getGlobalInstructions());

        try {
            emitFramePrologue();

            for (IRInstruction instruction : currentProgram.getGlobalInstructions()) {
                generateInstruction(instruction);
            }

            emitFrameEpilogue();
        } finally {
            currentFunction = previousFunction;
            insideGlobalInit = previousInsideGlobalInit;
            currentExitLabel = previousExitLabel;
        }
    }

    // Traduce una funcion IR completa.
    private void generateFunction(IRFunction function) {
        IRFunction previousFunction = currentFunction;
        boolean previousInsideGlobalInit = insideGlobalInit;
        String previousExitLabel = currentExitLabel;

        currentFunction = function;
        insideGlobalInit = false;
        currentExitLabel = "__exit_" + function.getName();
        prepareFunctionFrame(function);

        try {
            emitTextLine("");
            emitTextLine(function.getName() + ":");
            emitFramePrologue();
            moveParametersIntoFrame(function);

            for (IRInstruction instruction : function.getInstructions()) {
                generateInstruction(instruction);
            }

            emitTextLine(currentExitLabel + ":");
            emitFrameEpilogue();
        } finally {
            currentFunction = previousFunction;
            insideGlobalInit = previousInsideGlobalInit;
            currentExitLabel = previousExitLabel;
        }
    }

    // Copia los parametros desde registros/pila al frame local.
    private void moveParametersIntoFrame(IRFunction function) {
        for (int i = 0; i < function.getParameters().size(); i++) {
            IRParameter parameter = function.getParameters().get(i);
            int offset = requireStackSlot(parameter.getName());

            if (i < 4) {
                emitTextLine("sw $a" + i + ", " + offset + "($fp)");
                continue;
            }

            int callerOffset = SAVED_FRAME_BYTES + ((i - 4) * WORD_SIZE);
            emitTextLine("lw $t0, " + callerOffset + "($fp)");
            emitTextLine("sw $t0, " + offset + "($fp)");
        }
    }

    // Emite el prologo comun de un frame con pila local.
    private void emitFramePrologue() {
        emitTextLine("addi $sp, $sp, -" + SAVED_FRAME_BYTES);
        emitTextLine("sw $ra, 4($sp)");
        emitTextLine("sw $fp, 0($sp)");
        emitTextLine("move $fp, $sp");

        if (currentLocalStackSize > 0) {
            emitTextLine("addi $sp, $sp, -" + currentLocalStackSize);
        }
    }

    // Emite el epilogo comun de un frame con pila local.
    private void emitFrameEpilogue() {
        emitTextLine("move $sp, $fp");
        emitTextLine("lw $fp, 0($sp)");
        emitTextLine("lw $ra, 4($sp)");
        emitTextLine("addi $sp, $sp, " + SAVED_FRAME_BYTES);
        emitTextLine("jr $ra");
    }

    // Despacha la traduccion segun el tipo de instruccion IR.
    private void generateInstruction(IRInstruction instruction) {
        if (instruction instanceof AssignInstruction assignInstruction) {
            generateAssignInstruction(assignInstruction);
            return;
        }

        if (instruction instanceof BinaryInstruction binaryInstruction) {
            generateBinaryInstruction(binaryInstruction);
            return;
        }

        if (instruction instanceof UnaryInstruction unaryInstruction) {
            generateUnaryInstruction(unaryInstruction);
            return;
        }

        if (instruction instanceof CallInstruction callInstruction) {
            generateCallInstruction(callInstruction);
            return;
        }

        if (instruction instanceof PrintInstruction printInstruction) {
            generatePrintInstruction(printInstruction);
            return;
        }

        if (instruction instanceof ReturnInstruction returnInstruction) {
            generateReturnInstruction(returnInstruction);
            return;
        }

        if (instruction instanceof GotoInstruction gotoInstruction) {
            generateGotoInstruction(gotoInstruction);
            return;
        }

        if (instruction instanceof IfFalseGotoInstruction ifFalseGotoInstruction) {
            generateIfFalseGotoInstruction(ifFalseGotoInstruction);
            return;
        }

        if (instruction instanceof LabelInstruction labelInstruction) {
            generateLabelInstruction(labelInstruction);
            return;
        }

        throw new MipsGenerationException(instruction, "Instruccion IR no soportada en el backend MIPS");
    }

    private void generateAssignInstruction(AssignInstruction instruction) {
        moveOperandToTarget(instruction.getValue(), instruction.getTarget());
    }

    private void generateBinaryInstruction(BinaryInstruction instruction) {
        switch (instruction.getOperator()) {
            case "+", "-", "*", "/" -> generateArithmeticBinaryInstruction(instruction);
            case "<", ">", "<=", ">=" -> generateRelationalBinaryInstruction(instruction);
            case "==" -> generateEqualityBinaryInstruction(instruction);
            case "y", "o" -> generateLogicalBinaryInstruction(instruction);
            default -> throw new MipsGenerationException(instruction,
                    "Operador binario no soportado en backend MIPS: " + instruction.getOperator());
        }
    }

    private void generateUnaryInstruction(UnaryInstruction instruction) {
        ValueType targetType = getOperandType(instruction.getTarget());

        switch (instruction.getOperator()) {
            case "+" -> moveOperandToTarget(instruction.getValue(), instruction.getTarget());
            case "-" -> {
                if (isFloatType(targetType)) {
                    loadNumericOperandAsFloat(instruction.getValue(), "$f0");
                    emitTextLine("neg.s $f1, $f0");
                    storeFloatValue("$f1", instruction.getTarget());
                    return;
                }

                loadWordValue(instruction.getValue(), "$t0");
                emitTextLine("subu $t1, $zero, $t0");
                storeWordValue("$t1", instruction.getTarget());
            }
            default -> throw new MipsGenerationException(instruction,
                    "Operador unario no soportado en backend MIPS: " + instruction.getOperator());
        }
    }

    private void generateCallInstruction(CallInstruction instruction) {
        IRFunction callee = functionsByName.get(instruction.getFunctionName());
        if (callee == null) {
            throw new MipsGenerationException(instruction,
                    "No se encontro la funcion '" + instruction.getFunctionName() + "' en el programa IR");
        }

        List<IROperand> arguments = instruction.getArguments();
        if (arguments.size() != callee.getParameters().size()) {
            throw new MipsGenerationException(instruction,
                    "La llamada a '" + instruction.getFunctionName() + "' no coincide con su firma IR");
        }

        int extraArgumentCount = Math.max(0, arguments.size() - 4);
        int extraArgumentBytes = extraArgumentCount * WORD_SIZE;

        if (extraArgumentBytes > 0) {
            emitTextLine("addi $sp, $sp, -" + extraArgumentBytes);
        }

        for (int i = 0; i < arguments.size(); i++) {
            ValueType expectedType = callee.getParameters().get(i).getType();
            materializeArgumentWord(arguments.get(i), expectedType, "$t0");

            if (i < 4) {
                emitTextLine("move $a" + i + ", $t0");
                continue;
            }

            emitTextLine("sw $t0, " + ((i - 4) * WORD_SIZE) + "($sp)");
        }

        emitTextLine("jal " + instruction.getFunctionName());

        if (extraArgumentBytes > 0) {
            emitTextLine("addi $sp, $sp, " + extraArgumentBytes);
        }

        if (instruction.getTarget() == null) {
            return;
        }

        if (isFloatType(getOperandType(instruction.getTarget()))) {
            emitTextLine("mtc1 $v0, $f0");
            storeFloatValue("$f0", instruction.getTarget());
            return;
        }

        storeWordValue("$v0", instruction.getTarget());
    }

    private void generatePrintInstruction(PrintInstruction instruction) {
        throw new MipsGenerationException(instruction, "di! no esta soportado en el backend MIPS");
    }

    private void generateReturnInstruction(ReturnInstruction instruction) {
        if (insideGlobalInit || currentFunction == null || currentExitLabel == null) {
            throw new MipsGenerationException(instruction,
                    "No se puede emitir un retorno fuera del cuerpo de una funcion");
        }

        if (instruction.getValue() != null) {
            materializeArgumentWord(instruction.getValue(), currentFunction.getReturnType(), "$v0");
        }

        emitTextLine("j " + currentExitLabel);
    }

    private void generateGotoInstruction(GotoInstruction instruction) {
        emitTextLine("j " + instruction.getTarget().getName());
    }

    private void generateIfFalseGotoInstruction(IfFalseGotoInstruction instruction) {
        loadWordValue(instruction.getCondition(), "$t0");
        emitTextLine("beq $t0, $zero, " + instruction.getTarget().getName());
    }

    private void generateLabelInstruction(LabelInstruction instruction) {
        emitTextLine(instruction.getLabel().getName() + ":");
    }

    // Traduce una operacion aritmetica binaria.
    private void generateArithmeticBinaryInstruction(BinaryInstruction instruction) {
        ValueType targetType = getOperandType(instruction.getTarget());

        if (isFloatType(targetType)) {
            loadNumericOperandAsFloat(instruction.getLeft(), "$f0");
            loadNumericOperandAsFloat(instruction.getRight(), "$f1");

            switch (instruction.getOperator()) {
                case "+" -> emitTextLine("add.s $f2, $f0, $f1");
                case "-" -> emitTextLine("sub.s $f2, $f0, $f1");
                case "*" -> emitTextLine("mul.s $f2, $f0, $f1");
                case "/" -> emitTextLine("div.s $f2, $f0, $f1");
                default -> throw new MipsGenerationException(instruction,
                        "Operador aritmetico flotante no soportado: " + instruction.getOperator());
            }

            storeFloatValue("$f2", instruction.getTarget());
            return;
        }

        loadWordValue(instruction.getLeft(), "$t0");
        loadWordValue(instruction.getRight(), "$t1");

        switch (instruction.getOperator()) {
            case "+" -> emitTextLine("addu $t2, $t0, $t1");
            case "-" -> emitTextLine("subu $t2, $t0, $t1");
            case "*" -> emitTextLine("mul $t2, $t0, $t1");
            case "/" -> {
                emitTextLine("div $t0, $t1");
                emitTextLine("mflo $t2");
            }
            default -> throw new MipsGenerationException(instruction,
                    "Operador aritmetico entero no soportado: " + instruction.getOperator());
        }

        storeWordValue("$t2", instruction.getTarget());
    }

    // Traduce una comparacion relacional binaria.
    private void generateRelationalBinaryInstruction(BinaryInstruction instruction) {
        if (shouldUseFloatComparison(instruction.getLeft(), instruction.getRight())) {
            loadNumericOperandAsFloat(instruction.getLeft(), "$f0");
            loadNumericOperandAsFloat(instruction.getRight(), "$f1");

            switch (instruction.getOperator()) {
                case "<" -> emitTextLine("c.lt.s $f0, $f1");
                case "<=" -> emitTextLine("c.le.s $f0, $f1");
                case ">" -> emitTextLine("c.lt.s $f1, $f0");
                case ">=" -> emitTextLine("c.le.s $f1, $f0");
                default -> throw new MipsGenerationException(instruction,
                        "Operador relacional flotante no soportado: " + instruction.getOperator());
            }

            storeBooleanFromCop1Condition(instruction.getTarget());
            return;
        }

        loadWordValue(instruction.getLeft(), "$t0");
        loadWordValue(instruction.getRight(), "$t1");

        switch (instruction.getOperator()) {
            case "<" -> emitTextLine("slt $t2, $t0, $t1");
            case ">" -> emitTextLine("slt $t2, $t1, $t0");
            case "<=" -> {
                emitTextLine("slt $t2, $t1, $t0");
                emitTextLine("xori $t2, $t2, 1");
            }
            case ">=" -> {
                emitTextLine("slt $t2, $t0, $t1");
                emitTextLine("xori $t2, $t2, 1");
            }
            default -> throw new MipsGenerationException(instruction,
                    "Operador relacional entero no soportado: " + instruction.getOperator());
        }

        storeWordValue("$t2", instruction.getTarget());
    }

    // Traduce una comparacion de igualdad.
    private void generateEqualityBinaryInstruction(BinaryInstruction instruction) {
        ValueType leftType = getOperandType(instruction.getLeft());
        ValueType rightType = getOperandType(instruction.getRight());

        if (leftType == ValueType.CARRO && rightType == ValueType.CARRO) {
            loadWordValue(instruction.getLeft(), "$a0");
            loadWordValue(instruction.getRight(), "$a1");
            emitTextLine("jal " + STRING_EQUALS_LABEL);
            storeWordValue("$v0", instruction.getTarget());
            return;
        }

        if (shouldUseFloatComparison(instruction.getLeft(), instruction.getRight())) {
            loadNumericOperandAsFloat(instruction.getLeft(), "$f0");
            loadNumericOperandAsFloat(instruction.getRight(), "$f1");
            emitTextLine("c.eq.s $f0, $f1");
            storeBooleanFromCop1Condition(instruction.getTarget());
            return;
        }

        loadWordValue(instruction.getLeft(), "$t0");
        loadWordValue(instruction.getRight(), "$t1");
        emitTextLine("xor $t2, $t0, $t1");
        emitTextLine("sltiu $t2, $t2, 1");
        storeWordValue("$t2", instruction.getTarget());
    }

    // Traduce una operacion logica booleana.
    private void generateLogicalBinaryInstruction(BinaryInstruction instruction) {
        loadWordValue(instruction.getLeft(), "$t0");
        loadWordValue(instruction.getRight(), "$t1");

        switch (instruction.getOperator()) {
            case "y" -> emitTextLine("and $t2, $t0, $t1");
            case "o" -> emitTextLine("or $t2, $t0, $t1");
            default -> throw new MipsGenerationException(instruction,
                    "Operador logico no soportado: " + instruction.getOperator());
        }

        storeWordValue("$t2", instruction.getTarget());
    }

    // Mueve un operando al destino respetando conversiones permitidas.
    private void moveOperandToTarget(IROperand source, IROperand target) {
        ValueType targetType = getOperandType(target);

        if (isFloatType(targetType)) {
            loadValueAsType(source, targetType, "$t0", "$f0");
            storeFloatValue("$f0", target);
            return;
        }

        loadValueAsType(source, targetType, "$t0", "$f0");
        storeWordValue("$t0", target);
    }

    // Materializa un valor en registros segun el tipo esperado.
    private void loadValueAsType(IROperand operand, ValueType expectedType, String wordRegister, String floatRegister) {
        if (isFloatType(expectedType)) {
            loadNumericOperandAsFloat(operand, floatRegister);
            return;
        }

        loadWordValue(operand, wordRegister);
    }

    // Convierte un operando al formato palabra de una llamada o retorno.
    private void materializeArgumentWord(IROperand operand, ValueType expectedType, String register) {
        if (isFloatType(expectedType)) {
            loadNumericOperandAsFloat(operand, "$f0");
            emitTextLine("mfc1 " + register + ", $f0");
            return;
        }

        loadWordValue(operand, register);
    }

    // Carga un operando no flotante como valor de palabra.
    private void loadWordValue(IROperand operand, String register) {
        ValueType type = getOperandType(operand);
        if (isFloatType(type)) {
            throw new MipsGenerationException("No se puede cargar un valor aPachas en un registro entero sin conversion");
        }

        if (operand instanceof LiteralOperand literalOperand) {
            loadWordLiteral(literalOperand, register);
            return;
        }

        if (operand instanceof VariableOperand || operand instanceof TempOperand) {
            emitTextLine("lw " + register + ", " + resolveStorageLocation(operand));
            return;
        }

        throw new MipsGenerationException("No se puede cargar el operando IR en un registro MIPS: "
                + operand.getClass().getSimpleName());
    }

    // Carga un operando numerico como flotante, convirtiendo enteros cuando hace falta.
    private void loadNumericOperandAsFloat(IROperand operand, String floatRegister) {
        ValueType type = getOperandType(operand);
        if (isFloatType(type)) {
            loadFloatValue(operand, floatRegister);
            return;
        }

        if (type != ValueType.PAVOS) {
            throw new MipsGenerationException("Solo se pueden convertir a aPachas valores numericos pavos");
        }

        loadWordValue(operand, "$t0");
        emitTextLine("mtc1 $t0, " + floatRegister);
        emitTextLine("cvt.s.w " + floatRegister + ", " + floatRegister);
    }

    // Carga un valor float puro en un registro de coprocesador.
    private void loadFloatValue(IROperand operand, String floatRegister) {
        if (operand instanceof LiteralOperand literalOperand) {
            emitTextLine("l.s " + floatRegister + ", " + ensureFloatLiteralLabel(literalOperand.getValue()));
            return;
        }

        if (operand instanceof VariableOperand || operand instanceof TempOperand) {
            emitTextLine("l.s " + floatRegister + ", " + resolveStorageLocation(operand));
            return;
        }

        throw new MipsGenerationException("No se puede cargar el operando float IR en un registro MIPS: "
                + operand.getClass().getSimpleName());
    }

    // Almacena un valor palabra sobre un destino direccionable.
    private void storeWordValue(String register, IROperand operand) {
        if (isFloatType(getOperandType(operand))) {
            throw new MipsGenerationException("No se puede almacenar un registro entero sobre un destino aPachas");
        }

        emitTextLine("sw " + register + ", " + resolveStorageLocation(operand));
    }

    // Almacena un valor float sobre un destino direccionable.
    private void storeFloatValue(String floatRegister, IROperand operand) {
        if (!isFloatType(getOperandType(operand))) {
            throw new MipsGenerationException("No se puede almacenar un registro float sobre un destino no aPachas");
        }

        emitTextLine("s.s " + floatRegister + ", " + resolveStorageLocation(operand));
    }

    // Convierte una condicion de COP1 a un booleano entero 0/1.
    private void storeBooleanFromCop1Condition(IROperand target) {
        String trueLabel = newInternalLabel("cop1_true");
        String endLabel = newInternalLabel("cop1_end");

        emitTextLine("bc1t " + trueLabel);
        emitTextLine("li $t0, 0");
        emitTextLine("j " + endLabel);
        emitTextLine(trueLabel + ":");
        emitTextLine("li $t0, 1");
        emitTextLine(endLabel + ":");
        storeWordValue("$t0", target);
    }

    // Reserva el frame necesario para una funcion normal.
    private void prepareFunctionFrame(IRFunction function) {
        stackOffsets.clear();
        localTypes.clear();
        nextInternalLabelId = 0;
        currentLocalStackSize = 0;
        reserveKnownVariables(function.getParameters(), function.getLocalVariables());
        collectTemporaryTargets(function.getInstructions());
        currentLocalStackSize = -nextAvailableOffset();
    }

    // Reserva el frame temporal necesario para inicializadores globales.
    private void prepareGlobalInitFrame(List<IRInstruction> instructions) {
        stackOffsets.clear();
        localTypes.clear();
        nextInternalLabelId = 0;
        currentLocalStackSize = 0;
        collectTemporaryTargets(instructions);
        currentLocalStackSize = -nextAvailableOffset();
    }

    // Reserva slots para parametros y variables locales declaradas.
    private void reserveKnownVariables(List<IRParameter> parameters, List<IRVariable> localVariables) {
        for (IRParameter parameter : parameters) {
            registerLocalStorage(parameter.getName(), parameter.getType());
        }

        for (IRVariable localVariable : localVariables) {
            registerLocalStorage(localVariable.getName(), localVariable.getType());
        }
    }

    // Recorre las instrucciones para reservar slots de temporales.
    private void collectTemporaryTargets(List<IRInstruction> instructions) {
        for (IRInstruction instruction : instructions) {
            if (instruction instanceof AssignInstruction assignInstruction) {
                registerTargetStorage(assignInstruction.getTarget());
                continue;
            }

            if (instruction instanceof BinaryInstruction binaryInstruction) {
                registerTargetStorage(binaryInstruction.getTarget());
                continue;
            }

            if (instruction instanceof UnaryInstruction unaryInstruction) {
                registerTargetStorage(unaryInstruction.getTarget());
                continue;
            }

            if (instruction instanceof CallInstruction callInstruction && callInstruction.getTarget() != null) {
                registerTargetStorage(callInstruction.getTarget());
            }
        }
    }

    // Registra almacenamiento local si el destino lo necesita.
    private void registerTargetStorage(IROperand operand) {
        if (operand instanceof TempOperand tempOperand) {
            registerLocalStorage(tempOperand.getName(), getOperandType(tempOperand));
            return;
        }

        if (!(operand instanceof VariableOperand variableOperand)) {
            return;
        }

        if (localTypes.containsKey(variableOperand.getName())) {
            reserveStackSlot(variableOperand.getName());
        }
    }

    // Registra un simbolo local con su tipo y slot.
    private void registerLocalStorage(String name, ValueType type) {
        if (!localTypes.containsKey(name)) {
            localTypes.put(name, type);
        }

        reserveStackSlot(name);
    }

    // Reserva o recupera el offset de pila asociado a un nombre local.
    private int reserveStackSlot(String operandName) {
        Integer existingOffset = stackOffsets.get(operandName);
        if (existingOffset != null) {
            return existingOffset;
        }

        int offset = -WORD_SIZE * (stackOffsets.size() + 1);
        stackOffsets.put(operandName, offset);
        return offset;
    }

    // Devuelve el offset obligatorio de un nombre ya reservado.
    private int requireStackSlot(String operandName) {
        Integer offset = stackOffsets.get(operandName);
        if (offset == null) {
            throw new MipsGenerationException("No se encontro slot de pila para '" + operandName + "'");
        }

        return offset;
    }

    // Devuelve el ultimo offset usado para calcular el tamano del frame.
    private int nextAvailableOffset() {
        int lowestOffset = 0;

        for (int offset : stackOffsets.values()) {
            if (offset < lowestOffset) {
                lowestOffset = offset;
            }
        }

        return lowestOffset;
    }

    // Declara una variable global simple en la seccion de datos.
    private void declareGlobalStorage(IRVariable globalVariable) {
        String label = globalLabels.get(globalVariable.getName());
        if (label == null) {
            throw new MipsGenerationException("No se encontro etiqueta global para '" + globalVariable.getName() + "'");
        }

        if (isFloatType(globalVariable.getType())) {
            emitDataLine(label + ": .float 0.0");
            return;
        }

        emitDataLine(label + ": .word 0");
    }

    // Carga un literal no flotante.
    private void loadWordLiteral(LiteralOperand literalOperand, String register) {
        ValueType type = getOperandType(literalOperand);

        if (type == ValueType.CARRO) {
            emitTextLine("la " + register + ", " + ensureStringLiteralLabel(literalOperand.getValue()));
            return;
        }

        if (type == ValueType.LETRILLA) {
            emitTextLine("li " + register + ", " + decodeCharLiteral(literalOperand.getValue()));
            return;
        }

        if (type == ValueType.JURA) {
            emitTextLine("li " + register + ", " + ("jurao".equals(literalOperand.getValue()) ? "1" : "0"));
            return;
        }

        if (type == ValueType.PAVOS) {
            emitTextLine("li " + register + ", " + literalOperand.getValue());
            return;
        }

        throw new MipsGenerationException("Literal no soportado como palabra MIPS: " + literalOperand.getValue());
    }

    // Reserva o recupera el label asociado a un literal de cadena.
    private String ensureStringLiteralLabel(String literal) {
        if (stringLabels.containsKey(literal)) {
            return stringLabels.get(literal);
        }

        String label = "str_" + nextStringId++;
        stringLabels.put(literal, label);
        emitDataLine(label + ": .asciiz " + literal);
        return label;
    }

    // Reserva o recupera el label asociado a un literal float.
    private String ensureFloatLiteralLabel(String literal) {
        if (floatLabels.containsKey(literal)) {
            return floatLabels.get(literal);
        }

        String label = "flt_" + nextFloatId++;
        floatLabels.put(literal, label);
        emitDataLine(label + ": .float " + literal);
        return label;
    }

    // Emite un helper para comparar strings por contenido.
    private void emitStringEqualsHelper() {
        emitTextLine("");
        emitTextLine(STRING_EQUALS_LABEL + ":");
        emitTextLine("beq $a0, $a1, " + STRING_EQUALS_LABEL + "_true");
        emitTextLine("beq $a0, $zero, " + STRING_EQUALS_LABEL + "_false");
        emitTextLine("beq $a1, $zero, " + STRING_EQUALS_LABEL + "_false");
        emitTextLine(STRING_EQUALS_LABEL + "_loop:");
        emitTextLine("lb $t0, 0($a0)");
        emitTextLine("lb $t1, 0($a1)");
        emitTextLine("bne $t0, $t1, " + STRING_EQUALS_LABEL + "_false");
        emitTextLine("beq $t0, $zero, " + STRING_EQUALS_LABEL + "_true");
        emitTextLine("addi $a0, $a0, 1");
        emitTextLine("addi $a1, $a1, 1");
        emitTextLine("j " + STRING_EQUALS_LABEL + "_loop");
        emitTextLine(STRING_EQUALS_LABEL + "_true:");
        emitTextLine("li $v0, 1");
        emitTextLine("jr $ra");
        emitTextLine(STRING_EQUALS_LABEL + "_false:");
        emitTextLine("move $v0, $zero");
        emitTextLine("jr $ra");
    }

    // Resuelve un operando IR a su almacenamiento direccionable.
    private String resolveStorageLocation(IROperand operand) {
        if (operand instanceof TempOperand tempOperand) {
            return requireStackSlot(tempOperand.getName()) + "($fp)";
        }

        if (!(operand instanceof VariableOperand variableOperand)) {
            throw new MipsGenerationException("El operando no tiene almacenamiento direccionable en MIPS");
        }

        if (localTypes.containsKey(variableOperand.getName()) && stackOffsets.containsKey(variableOperand.getName())) {
            return requireStackSlot(variableOperand.getName()) + "($fp)";
        }

        String globalLabel = globalLabels.get(variableOperand.getName());
        if (globalLabel != null) {
            return globalLabel;
        }

        throw new MipsGenerationException("No se encontro almacenamiento MIPS para la variable '" + variableOperand.getName() + "'");
    }

    // Devuelve el tipo conocido de un operando IR.
    private ValueType getOperandType(IROperand operand) {
        if (operand instanceof VariableOperand variableOperand) {
            if (variableOperand.getType() != null) {
                return variableOperand.getType();
            }

            ValueType localType = localTypes.get(variableOperand.getName());
            if (localType != null) {
                return localType;
            }

            ValueType globalType = globalTypes.get(variableOperand.getName());
            if (globalType != null) {
                return globalType;
            }
        }

        if (operand instanceof TempOperand tempOperand) {
            if (tempOperand.getType() != null) {
                return tempOperand.getType();
            }

            ValueType localType = localTypes.get(tempOperand.getName());
            if (localType != null) {
                return localType;
            }
        }

        if (operand instanceof LiteralOperand literalOperand) {
            if (literalOperand.getType() != null) {
                return literalOperand.getType();
            }

            return inferLiteralType(literalOperand.getValue());
        }

        throw new MipsGenerationException("No se pudo resolver el tipo del operando IR: "
                + operand.getClass().getSimpleName());
    }

    // Decide si una comparacion numerica debe hacerse en coma flotante.
    private boolean shouldUseFloatComparison(IROperand left, IROperand right) {
        return isFloatType(getOperandType(left)) || isFloatType(getOperandType(right));
    }

    // Detecta si el tipo corresponde a aPachas.
    private boolean isFloatType(ValueType type) {
        return type != null && type.isFloat();
    }

    // Deduce el tipo de un literal cuando no viene anotado.
    private ValueType inferLiteralType(String literal) {
        if ("jurao".equals(literal) || "bulo".equals(literal)) {
            return ValueType.JURA;
        }

        if (literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"")) {
            return ValueType.CARRO;
        }

        if (literal.length() >= 3 && literal.startsWith("'") && literal.endsWith("'")) {
            return ValueType.LETRILLA;
        }

        if (literal.contains(".") || literal.contains("e") || literal.contains("E")) {
            return ValueType.APACHAS;
        }

        return ValueType.PAVOS;
    }

    // Decodifica un literal de caracter IR al codigo entero correspondiente.
    private int decodeCharLiteral(String literal) {
        String body = literal.substring(1, literal.length() - 1);
        if (body.isEmpty()) {
            throw new MipsGenerationException("Literal de caracter vacio en backend MIPS");
        }

        if (body.length() == 1 && body.charAt(0) != '\\') {
            return body.charAt(0);
        }

        if (body.length() == 2 && body.charAt(0) == '\\') {
            return switch (body.charAt(1)) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case '\'' -> '\'';
                case '"' -> '"';
                case '\\' -> '\\';
                default -> throw new MipsGenerationException(
                        "Secuencia de escape de caracter no soportada en backend MIPS: " + literal
                );
            };
        }

        throw new MipsGenerationException("Literal de caracter no soportado en backend MIPS: " + literal);
    }

    // Genera una etiqueta interna unica para helpers de control.
    private String newInternalLabel(String prefix) {
        return "__" + prefix + "_" + nextInternalLabelId++;
    }

    // Agrega una linea cruda a la seccion de datos.
    private void emitDataLine(String line) {
        dataSection.append(line).append(System.lineSeparator());
    }

    // Agrega una linea cruda a la seccion de texto.
    private void emitTextLine(String line) {
        textSection.append(line).append(System.lineSeparator());
    }
}
