package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums;

/**
 * Las constantes están en inglés, pero los valores de metodo_checkin_enum siguen en español: el
 * esquema no se toca. La traducción la hace {@link CheckinMethodConverter}, y hacia los
 * procedimientos almacenados hay que pasar {@link #getDbValue()}, nunca name().
 */
public enum CheckinMethod {
    QR("QR"),
    DOCUMENT("DOCUMENTO"),
    MANUAL("MANUAL");

    private final String dbValue;

    CheckinMethod(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CheckinMethod fromDbValue(String dbValue) {
        for (CheckinMethod value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para metodo_checkin_enum: " + dbValue);
    }
}
