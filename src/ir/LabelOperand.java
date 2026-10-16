package ir;

/**
 * Operando que representa una etiqueta de salto.
 */
public class LabelOperand implements IROperand {
    private final String name;

    public LabelOperand(String name) {
        this.name = name;
    }

    // Devuelve el nombre de la etiqueta.
    public String getName() {
        return name;
    }

    @Override
    public String format() {
        return name;
    }
}
