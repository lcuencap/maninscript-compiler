package ast.statements;

import ast.expressions.CallExprNode;

/**
 * Sentencia que invoca una funcion sin usar su valor de retorno.
 */
public class CallStatementNode extends StatementNode {
    private final CallExprNode call;

    public CallStatementNode(CallExprNode call) {
        this.call = call;
    }

    // Devuelve la llamada ejecutada por la sentencia.
    public CallExprNode getCall() {
        return call;
    }
}
