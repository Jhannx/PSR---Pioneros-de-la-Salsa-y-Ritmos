package com.jhanp.psraerobicsbackend.auth.token.enums;

/**
 * Por qué se emitió un token. El mecanismo es idéntico en los dos casos —se emite, se manda por
 * correo y se canjea una sola vez—; lo único que cambia es cuánto dura y qué plantilla se envía.
 * <p>
 * Las constantes están en inglés y los valores guardados en español, como el resto del esquema. La
 * traducción la hace {@link TokenTypeConverter}.
 */
public enum TokenType {

    /** El alta creó la cuenta y se invita a fijar una contraseña propia. Ventana larga. */
    INVITATION("INVITACION"),

    /** "Olvidé mi contraseña", pedido por la propia persona. Ventana corta. */
    RECOVERY("RECUPERACION");

    private final String dbValue;

    TokenType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static TokenType fromDbValue(String dbValue) {
        for (TokenType value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para token_recuperacion.tipo: " + dbValue);
    }
}
