package types;

import lexer.TokenType;

/**
 * Representa los tipos de valor del lenguaje y algunos helpers comunes.
 */
public enum ValueType {
    PAVOS("pavos"),
    APACHAS("aPachas"),
    CARRO("carro"),
    LETRILLA("letrilla"),
    JURA("jura"),
    NA_DE_NA("naDeNa"),
    ERROR("<error>");

    private final String sourceName;

    ValueType(String sourceName) {
        this.sourceName = sourceName;
    }

    // Convierte un token de tipo del parser al enum interno.
    public static ValueType fromTypeToken(TokenType tokenType) {
        return switch (tokenType) {
            case PAVOS -> PAVOS;
            case APACHAS -> APACHAS;
            case CARRO -> CARRO;
            case JURA -> JURA;
            case LETRILLA -> LETRILLA;
            case NA_DE_NA -> NA_DE_NA;
            default -> throw new IllegalArgumentException("Token de tipo no soportado: " + tokenType);
        };
    }

    // Indica si el tipo es numerico.
    public boolean isNumeric() {
        return this == PAVOS || this == APACHAS;
    }

    // Indica si el tipo representa un float.
    public boolean isFloat() {
        return this == APACHAS;
    }

    // Indica si el tipo representa un booleano.
    public boolean isBoolean() {
        return this == JURA;
    }

    // Indica si el tipo representa una cadena.
    public boolean isString() {
        return this == CARRO;
    }

    // Indica si el tipo representa un char.
    public boolean isChar() {
        return this == LETRILLA;
    }

    // Indica si el tipo representa un retorno vacio.
    public boolean isVoid() {
        return this == NA_DE_NA;
    }

    // Indica si el tipo es el sentinel interno de error.
    public boolean isError() {
        return this == ERROR;
    }

    // Comprueba si este tipo puede recibir un valor del tipo indicado.
    public boolean accepts(ValueType actualType) {
        if (actualType == null || actualType.isError()) {
            return true;
        }

        if (this == actualType) {
            return true;
        }

        return this == APACHAS && actualType == PAVOS;
    }

    // Comprueba si dos tipos pueden compararse entre si.
    public boolean isComparableWith(ValueType otherType) {
        if (otherType == null || this.isError() || otherType.isError()) {
            return true;
        }

        if (this == otherType) {
            return true;
        }

        return this.isNumeric() && otherType.isNumeric();
    }

    // Devuelve el tipo numerico promovido entre dos operandos.
    public static ValueType promoteNumeric(ValueType leftType, ValueType rightType) {
        if (leftType == APACHAS || rightType == APACHAS) {
            return APACHAS;
        }

        return PAVOS;
    }

    @Override
    public String toString() {
        return sourceName;
    }
}
