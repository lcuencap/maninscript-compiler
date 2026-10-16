package ast;

import java.util.List;

/**
 * Nodo raiz que agrupa los elementos globales del programa.
 */
public class ProgramNode extends ASTNode {
    private final List<GlobalElement> globalElements;

    public ProgramNode(List<GlobalElement> globalElements) {
        this.globalElements = globalElements;
    }

    // Devuelve los elementos globales del programa.
    public List<GlobalElement> getGlobalElements() {
        return globalElements;
    }
}
