package com.jhanp.psraerobicsbackend.modules.person.enums;

/**
 * Las constantes están en inglés, pero los valores de fuente_registro_enum siguen en español: el
 * esquema no se toca. La traducción la hace {@link RegistrationSourceConverter}, y hacia los
 * procedimientos almacenados hay que pasar {@link #getDbValue()}, nunca name().
 */
public enum RegistrationSource {
    ADMIN("ADMIN"),
    SELF_REGISTRATION("AUTOREGISTRO");

    private final String dbValue;

    RegistrationSource(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static RegistrationSource fromDbValue(String dbValue) {
        for (RegistrationSource value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para fuente_registro_enum: " + dbValue);
    }
}
