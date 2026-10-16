package ast.statements;

import ast.expressions.ExpressionNode;

/**
 * Sentencia condicional con rama alternativa opcional.
 */
public class IfNode extends StatementNode {
    private final ExpressionNode condition;
    private final BlockNode thenBlock;
    private final BlockNode elseBlock;

    public IfNode(ExpressionNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    // Devuelve la condicion del if.
    public ExpressionNode getCondition() { return condition; }

    // Devuelve el bloque que se ejecuta si la condicion es cierta.
    public BlockNode getThenBlock() { return thenBlock; }

    // Devuelve el bloque alternativo, si existe.
    public BlockNode getElseBlock() { return elseBlock; }
}
