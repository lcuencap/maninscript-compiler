import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SmokeTestRunner {
    private static final Path OUT_DIR = Path.of("out");

    public static void main(String[] args) throws Exception {
        compileSources();
        runValidCases();
        runInvalidCases();
        runMipsCases();
        System.out.println();
        System.out.println("Todas las pruebas pasaron correctamente.");
    }

    private static void compileSources() throws IOException {
        System.out.println("Compilando fuentes...");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No se encontro javac. Ejecuta las pruebas con un JDK, no solo con un JRE.");
        }

        Files.createDirectories(OUT_DIR);

        List<String> arguments = new ArrayList<>();
        arguments.add("-d");
        arguments.add(OUT_DIR.toString());

        try (var stream = Files.walk(Path.of("src"))) {
            stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(Path::toString)
                    .forEach(arguments::add);
        }

        int exitCode = compiler.run(null, System.out, System.err, arguments.toArray(String[]::new));
        if (exitCode != 0) {
            throw new AssertionError("La compilacion con javac fallo con codigo " + exitCode);
        }
    }

    private static void runValidCases() throws IOException, InterruptedException {
        System.out.println();
        System.out.println("Ejecutando casos validos...");

        List<ValidCase> cases = List.of(
                valid("fibonacci", "tests/cases/valid/fibonacci.ms",
                        expected("function fibRec(pavos n) : pavos",
                                "t2 = call fibRec(t1)",
                                "t4 = call fibRec(t3)",
                                "function fibIter(pavos n) : pavos",
                                "tmp = t8",
                                "function main() : naDeNa",
                                "t10 = call fibRec(6)",
                                "recursivo = t10",
                                "t11 = call fibIter(6)",
                                "iterativo = t11")),
                valid("while counter", "tests/cases/valid/while_counter.txt",
                        expected("globals",
                                "limite = 3",
                                "function cuenta() : naDeNa",
                                "L0:",
                                "ifFalse",
                                "print \"i\", i")),
                valid("unary and literals", "tests/cases/valid/unary_literals.txt",
                        expected("function ajusta(pavos x) : pavos",
                                "inicial = 'a'",
                                "texto = \"texto\"",
                                " = -x",
                                "print \"ok\", inicial, texto",
                                "return")),
                valid("if without else", "tests/cases/valid/if_without_else.txt",
                        expected("function revisa(pavos x) : naDeNa",
                                "ifFalse",
                                "print \"cero\"",
                                "print \"fin\"")),
                valid("string escapes", "tests/cases/valid/string_escapes.txt",
                        expected("function muestra() : naDeNa",
                                "texto = \"hola\\nmundo\"",
                                "cita = \"dijo \\\"hola\\\"\"",
                                "salto = '\\n'",
                                "print texto, cita, salto")),
                valid("forward call with comments", "tests/cases/valid/forward_call_with_comments.txt",
                        expected("function usa() : pavos",
                                "t0 = call dame()",
                                "base = t0",
                                "return base",
                                "function dame() : pavos",
                                "return 7")),
                valid("call as statement", "tests/cases/valid/call_statement.txt",
                        expected("function clasifica(pavos x) : naDeNa",
                                "print x",
                                "function main() : naDeNa",
                                "n1 = 5",
                                "call clasifica(n1)",
                                "call clasifica(3)")),
                valid("all return branches", "tests/cases/valid/all_return_branches.txt",
                        expected("function decide(jura ok) : pavos",
                                "ifFalse ok goto",
                                "return 1",
                                "return 2")),
                valid("optimizer removes dead code", "tests/cases/valid/optimizer_dead_code.txt",
                        expected("function corta() : pavos",
                                "return 1"),
                        absent("muerto = 2",
                                "print \"invisible\"")),
                valid("accented segunVea", "tests/cases/valid/accented_segunvea.txt",
                        expected("function cuentaAcentos() : pavos",
                                "ifFalse",
                                "i = 0",
                                "i = t1",
                                "return i")),
                valid("deep nesting stress", "tests/cases/valid/deep_nesting_stress.txt",
                        expected("function acumula(pavos limite) : pavos",
                                "L0:",
                                "L2:",
                                "t3 = i + j",
                                "ifFalse t4 goto L4",
                                "ifFalse t5 goto L4",
                                "return total"),
                        absent(" y ")),
                valid("short-circuit logic", "tests/cases/valid/short_circuit_logic.txt",
                        expected("function pruebaAnd() : pavos",
                                "ifFalse bulo goto L0",
                                "t1 = call rhs()",
                                "ifFalse t1 goto L0",
                                "function pruebaOr() : pavos",
                                "ifFalse jurao goto L3",
                                "t3 = call rhs()",
                                "ifFalse t3 goto L4"),
                        absent(" bulo y ",
                                " jurao o ")),
                valid("shadowing keeps distinct IR names", "tests/cases/valid/shadowing_ir_mips.txt",
                        expected("function sombraLocal() : pavos",
                                "valor = 1",
                                "valor__0 = 2",
                                "valor__0 = t0",
                                "return valor",
                                "function sombraGlobal() : pavos",
                                "x__1 = 2",
                                "x__1 = t1",
                                "return x")),
                valid("float arithmetic and promotion", "tests/cases/valid/float_arithmetic.txt",
                        expected("globals",
                                "global = 1.5",
                                "function escala(aPachas x, pavos n) : aPachas",
                                "factor = n",
                                "t0 = x * factor",
                                "t1 = t0 + global",
                                "ifFalse",
                                "t3 = total / 2.0",
                                "t4 = total + 0.25",
                                "return t5",
                                "function main() : aPachas",
                                "t6 = call escala(2.0, 3)",
                                "return resultado")),
                valid("maninScript .ms extension", "tests/cases/valid/minimal_ms_extension.ms",
                        expected("globals",
                                "base = 4",
                                "function main() : pavos",
                                "t0 = base * 2",
                                "return doble"))
        );

        for (ValidCase testCase : cases) {
            CommandResult result = runMain(testCase.path(), "--ir-only");
            assertExitCode(result.exitCode(), 0, testCase.name());
            assertNotContains(result.output(), List.of("Error "), testCase.name());
            assertContains(result.output(), testCase.expected(), testCase.name());
            assertNotContains(result.output(), testCase.absent(), testCase.name());
            System.out.println("  OK  " + testCase.name());
        }
    }

    private static void runInvalidCases() throws IOException, InterruptedException {
        System.out.println();
        System.out.println("Ejecutando casos invalidos...");

        List<InvalidCase> cases = List.of(
                invalid("const assignment", "tests/cases/invalid/invalid_const_assignment.txt",
                        expected("No se puede modificar la constante 'base'")),
                invalid("missing return", "tests/cases/invalid/invalid_missing_return.txt",
                        expected("debe devolver un valor de tipo pavos")),
                invalid("undeclared variable", "tests/cases/invalid/invalid_undeclared_var.txt",
                        expected("Variable no declarada: x")),
                invalid("type mismatch", "tests/cases/invalid/invalid_type_mismatch.txt",
                        expected("No se puede inicializar 'x' de tipo pavos con un valor de tipo carro")),
                invalid("float to int mismatch", "tests/cases/invalid/invalid_float_to_int.txt",
                        expected("No se puede inicializar 'x' de tipo pavos con un valor de tipo aPachas")),
                invalid("invalid condition", "tests/cases/invalid/invalid_condition.txt",
                        expected("La condicion de 'fijateSi' debe ser de tipo jura")),
                invalid("di without bang", "tests/cases/invalid/invalid_di_without_bang.txt",
                        expected("Funcion no declarada: di")),
                invalid("multiple syntax errors", "tests/cases/invalid/invalid_multiple_syntax_errors.txt",
                        expected("';' despues de la declaracion global",
                                "el identificador del parametro",
                                "';' despues de la sentencia",
                                "Token encontrado: ';'",
                                "Token encontrado: ')'")),
                invalid("call statement with return value", "tests/cases/invalid/invalid_call_statement_return_value.txt",
                        expected("devuelve pavos y no puede usarse como sentencia")),
                invalid("multiple semantic errors", "tests/cases/invalid/invalid_multiple_semantic_errors.txt",
                        expected("No se puede inicializar 'base' de tipo pavos con un valor de tipo carro",
                                "Parametro duplicado: x",
                                "Variable no declarada: z",
                                "No se puede modificar la constante 'tope'",
                                "La condicion de 'fijateSi' debe ser de tipo jura",
                                "La funcion 'rompe' debe devolver pavos, pero se encontro carro")),
                invalid("unclosed block comment", "tests/cases/invalid/invalid_unclosed_block_comment.txt",
                        expected("Comentario de bloque sin cerrar.")),
                invalid("reserved word identifier", "tests/cases/invalid/invalid_reserved_word_identifier.txt",
                        expected("pero se encontro la palabra reservada 'fijateSi'")),
                invalid("self initializer", "tests/cases/invalid/invalid_self_initializer.txt",
                        expected("La variable 'x' no puede usarse en su propio inicializador")),
                invalid("void value in print", "tests/cases/invalid/invalid_void_value_in_print.txt",
                        expected("No se puede usar un valor de tipo naDeNa en 'di!'")),
                invalid("name conflicts", "tests/cases/invalid/invalid_name_conflicts.txt",
                        expected("La funcion ya esta declarada: repetida",
                                "El nombre 'repetida' ya lo usa una funcion")),
                invalid("invalid bang operator", "tests/cases/invalid/invalid_bang_operator.txt",
                        expected("El lenguaje no admite '!' suelto ni '!=', solo la palabra reservada 'di!'")),
                invalid("nested syntax stress", "tests/cases/invalid/invalid_nested_syntax_stress.txt",
                        expected("')' despues de la condicion del 'segunVea'",
                                "Token encontrado: ';'",
                                "Token encontrado: ','",
                                "';' despues de la sentencia 'di!'")),
                invalid("nested semantic stress", "tests/cases/invalid/invalid_nested_semantic_stress.txt",
                        expected("Parametro duplicado: ok",
                                "La variable 'tope' no puede usarse en su propio inicializador",
                                "Los operadores aritmeticos necesitan operandos numericos, pero se encontraron jura y pavos",
                                "Funcion no declarada: fantasma",
                                "Variable no declarada: faltaVar",
                                "No se puede modificar la constante 'tope'",
                                "No se puede asignar un valor de tipo carro a 'tope' de tipo pavos",
                                "La funcion 'semCaos' debe devolver pavos, pero se encontro carro"))
        );

        for (InvalidCase testCase : cases) {
            CommandResult result = runMain(testCase.path(), "--ir-only");
            assertNonZeroExitCode(result.exitCode(), testCase.name());
            assertContains(result.output(), testCase.expected(), testCase.name());
            System.out.println("  OK  " + testCase.name());
        }
    }

    private static void runMipsCases() throws IOException, InterruptedException {
        System.out.println();
        System.out.println("Ejecutando casos validos de backend MIPS sin di!...");

        List<MipsCase> cases = List.of(
                mips("forward call without print", "tests/cases/valid/forward_call_with_comments.txt",
                        expected(".text",
                                "__start:",
                                "jal usa",
                                "usa:",
                                "jal dame",
                                "dame:")),
                mips("all return branches without print", "tests/cases/valid/all_return_branches.txt",
                        expected("decide:",
                                "beq",
                                "li $v0, 1",
                                "li $v0, 2",
                                "__exit_decide:")),
                mips("accented segunVea without print", "tests/cases/valid/accented_segunvea.txt",
                        expected("cuentaAcentos:",
                                "slt",
                                "beq",
                                "j L0",
                                "__exit_cuentaAcentos:")),
                mips("deep nesting without print", "tests/cases/valid/deep_nesting_stress.txt",
                        expected("acumula:",
                                "slt",
                                "beq $t0, $zero, L4",
                                "j L0",
                                "__exit_acumula:"),
                        absent("and ")),
                mips("optimizer removes print before MIPS", "tests/cases/valid/optimizer_dead_code.txt",
                        expected("corta:",
                                "li $v0, 1",
                                "__exit_corta:"),
                        absent("di! no esta soportado",
                                "invisible")),
                mips("shadowing generates separate MIPS storage", "tests/cases/valid/shadowing_ir_mips.txt",
                        expected("global_x: .word 0",
                                "sombraLocal:",
                                "lw $v0, -4($fp)",
                                "sombraGlobal:",
                                "sw $t0, -4($fp)",
                                "lw $v0, global_x")),
                mips("short-circuit logic in MIPS", "tests/cases/valid/short_circuit_logic.txt",
                        expected("pruebaAnd:",
                                "beq $t0, $zero, L0",
                                "jal rhs",
                                "pruebaOr:",
                                "beq $t0, $zero, L3",
                                "j L5")),
                mips("float arithmetic in MIPS", "tests/cases/valid/float_arithmetic.txt",
                        expected("global_global: .float 0.0",
                                "flt_0: .float 1.5",
                                "escala:",
                                "cvt.s.w $f0, $f0",
                                "mul.s $f2, $f0, $f1",
                                "add.s $f2, $f0, $f1",
                                "c.le.s $f1, $f0",
                                "div.s $f2, $f0, $f1",
                                "add.s $f2, $f0, $f1",
                                "neg.s $f1, $f0",
                                "mfc1 $v0, $f0",
                                "main:",
                                "jal escala")),
                mips("maninScript .ms extension in MIPS", "tests/cases/valid/minimal_ms_extension.ms",
                        expected("global_base: .word 0",
                                "main:",
                                "mul",
                                "lw $v0",
                                "__exit_main:"))
        );

        for (MipsCase testCase : cases) {
            Path asmPath = asmPath(testCase.path());
            Files.deleteIfExists(asmPath);

            CommandResult result = runMain(testCase.path(), "--no-ast");
            assertExitCode(result.exitCode(), 0, testCase.name());
            assertNotContains(result.output(), List.of("Error ", "no esta soportado"), testCase.name());

            if (!Files.exists(asmPath)) {
                throw new AssertionError("La prueba MIPS '" + testCase.name()
                        + "' no genero el fichero ASM esperado: " + asmPath);
            }

            String asmOutput = Files.readString(asmPath);
            assertContains(asmOutput, testCase.expectedAsm(), testCase.name());
            assertNotContains(asmOutput, testCase.absentAsm(), testCase.name());
            System.out.println("  OK  " + testCase.name());
        }
    }

    private static CommandResult runMain(String sourcePath, String flag) throws IOException, InterruptedException {
        List<String> command = List.of(javaExecutable(), "-cp", OUT_DIR.toString(), "Main", sourcePath, flag);
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        byte[] outputBytes = process.getInputStream().readAllBytes();
        int exitCode = process.waitFor();
        return new CommandResult(exitCode, new String(outputBytes, Charset.defaultCharset()).trim());
    }

    private static String javaExecutable() {
        String executable = isWindows() ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static Path asmPath(String sourcePath) {
        Path path = Path.of(sourcePath);
        String fileName = path.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String asmFileName = extensionIndex > 0
                ? fileName.substring(0, extensionIndex) + ".asm"
                : fileName + ".asm";
        Path parent = path.getParent();
        return parent == null ? Path.of(asmFileName) : parent.resolve(asmFileName);
    }

    private static void assertContains(String output, List<String> expected, String caseName) {
        for (String snippet : expected) {
            if (!output.contains(snippet)) {
                throw new AssertionError("La prueba '" + caseName
                        + "' no contiene el fragmento esperado: " + snippet
                        + System.lineSeparator() + "Salida:" + System.lineSeparator() + output);
            }
        }
    }

    private static void assertNotContains(String output, List<String> unexpected, String caseName) {
        for (String snippet : unexpected) {
            if (output.contains(snippet)) {
                throw new AssertionError("La prueba '" + caseName
                        + "' contiene un fragmento inesperado: " + snippet
                        + System.lineSeparator() + "Salida:" + System.lineSeparator() + output);
            }
        }
    }

    private static void assertExitCode(int actual, int expected, String caseName) {
        if (actual != expected) {
            throw new AssertionError("La prueba '" + caseName + "' termino con codigo "
                    + actual + ", pero se esperaba " + expected);
        }
    }

    private static void assertNonZeroExitCode(int actual, String caseName) {
        if (actual == 0) {
            throw new AssertionError("La prueba '" + caseName
                    + "' termino con codigo 0, pero se esperaba un fallo");
        }
    }

    private static List<String> expected(String... values) {
        return Arrays.asList(values);
    }

    private static List<String> absent(String... values) {
        return Arrays.asList(values);
    }

    private static ValidCase valid(String name, String path, List<String> expected) {
        return new ValidCase(name, path, expected, List.of());
    }

    private static ValidCase valid(String name, String path, List<String> expected, List<String> absent) {
        return new ValidCase(name, path, expected, absent);
    }

    private static InvalidCase invalid(String name, String path, List<String> expected) {
        return new InvalidCase(name, path, expected);
    }

    private static MipsCase mips(String name, String path, List<String> expectedAsm) {
        return new MipsCase(name, path, expectedAsm, List.of());
    }

    private static MipsCase mips(String name, String path, List<String> expectedAsm, List<String> absentAsm) {
        return new MipsCase(name, path, expectedAsm, absentAsm);
    }

    private record CommandResult(int exitCode, String output) {
    }

    private record ValidCase(String name, String path, List<String> expected, List<String> absent) {
    }

    private record InvalidCase(String name, String path, List<String> expected) {
    }

    private record MipsCase(String name, String path, List<String> expectedAsm, List<String> absentAsm) {
    }
}
