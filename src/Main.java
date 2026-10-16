import ast.ASTVisualizer;
import ast.ProgramNode;
import ir.IRGenerator;
import ir.IRGenerationException;
import ir.IROptimizer;
import ir.IRPrinter;
import ir.IRProgram;
import lexer.Lexer;
import lexer.LexicalException;
import mips.MipsGenerationException;
import mips.MipsGenerator;
import parser.Parser;
import parser.ParserException;
import semantic.SemanticAnalyzer;
import semantic.SemanticException;
import semantic.SemanticInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Punto de entrada de la herramienta de compilacion.
 */
public class Main {
    private static final String IR_ONLY_FLAG = "--ir-only";
    private static final String NO_AST_FLAG = "--no-ast";
    private static final String USAGE = "Use: java Main <ruta-fichero> [" + IR_ONLY_FLAG + "|" + NO_AST_FLAG + "]";

    // Ejecuta el flujo completo: leer, parsear, analizar y visualizar.
    public static void main(String[] args) {
        Path sourcePath = resolveSourcePath(args);
        if (sourcePath == null) {
            System.err.println(USAGE);
            System.exit(1);
            return;
        }

        if (!Files.exists(sourcePath)) {
            System.err.println("Fichero no encontrado: " + sourcePath);
            System.exit(1);
            return;
        }

        boolean showAst = shouldShowAst(args);
        boolean generateMips = shouldGenerateMips(args);

        try {
            String source = Files.readString(sourcePath);
            Lexer lexer = new Lexer(source);
            Parser parser = new Parser(lexer);
            ProgramNode program = parser.parseProgram();

            if (parser.hasErrors()) {
                for (ParserException parserError : parser.getErrors()) {
                    System.err.println(parserError.getMessage());
                }
                System.exit(1);
                return;
            }

            SemanticAnalyzer semantic = new SemanticAnalyzer();
            SemanticInfo semanticInfo = semantic.analyze(program);

            if (semantic.hasErrors()) {
                for (SemanticException semanticError : semantic.getErrors()) {
                    System.err.println(semanticError.getMessage());
                }
                System.exit(1);
                return;
            }

            IRGenerator irGenerator = new IRGenerator(semanticInfo);
            IRProgram irProgram = irGenerator.generate(program);
            IRProgram optimizedIrProgram = new IROptimizer().optimize(irProgram);
            System.out.println(new IRPrinter().print(optimizedIrProgram));

            if (generateMips) {
                String mipsCode = new MipsGenerator().generate(optimizedIrProgram);
                Files.writeString(resolveAsmOutputPath(sourcePath), mipsCode);
            }

            if (showAst) {
                ASTVisualizer.show(program, source);
            }
        } catch (LexicalException | ParserException | SemanticException
                 | IRGenerationException | MipsGenerationException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("Error de lectura del fichero: " + ex.getMessage());
            System.exit(1);
        }
    }

    // Devuelve el primer argumento posicional como ruta del programa fuente.
    private static Path resolveSourcePath(String[] args) {
        for (String arg : args) {
            if (!IR_ONLY_FLAG.equals(arg) && !NO_AST_FLAG.equals(arg)) {
                return Path.of(arg);
            }
        }
        return null;
    }

    // Genera ensamblador MIPS salvo cuando se pide explicitamente IR sin backend.
    private static boolean shouldGenerateMips(String[] args) {
        for (String arg : args) {
            if (IR_ONLY_FLAG.equals(arg)) {
                return false;
            }
        }

        return true;
    }

    // Permite validar la IR por consola sin abrir el visualizador Swing.
    private static boolean shouldShowAst(String[] args) {
        for (String arg : args) {
            if (IR_ONLY_FLAG.equals(arg) || NO_AST_FLAG.equals(arg)) {
                return false;
            }
        }

        return true;
    }

    // Genera la ruta de salida .asm a partir del nombre del fuente.
    private static Path resolveAsmOutputPath(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String asmFileName = extensionIndex > 0
                ? fileName.substring(0, extensionIndex) + ".asm"
                : fileName + ".asm";

        Path parent = sourcePath.getParent();
        if (parent == null) {
            return Path.of(asmFileName);
        }

        return parent.resolve(asmFileName);
    }
}
