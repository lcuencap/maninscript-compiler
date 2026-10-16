package ir;

/**
 * Contrato comun para las instrucciones del codigo intermedio.
 */
public interface IRInstruction {
    // Devuelve la instruccion en formato textual de tres direcciones.
    String format();
}
