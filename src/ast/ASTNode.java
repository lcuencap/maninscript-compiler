package ast;

/**
 * Nodo base del AST con informacion de posicion en el fuente.
 */
public abstract class ASTNode {
    private int line = -1;
    private int column = -1;

    // Devuelve la linea de origen del nodo.
    public int getLine() { return line; }

    // Devuelve la columna de origen del nodo.
    public int getColumn() { return column; }

    // Guarda la posicion del token que creo el nodo.
    public void setPosition(int line, int column) {
        this.line = line;
        this.column = column;
    }
}
