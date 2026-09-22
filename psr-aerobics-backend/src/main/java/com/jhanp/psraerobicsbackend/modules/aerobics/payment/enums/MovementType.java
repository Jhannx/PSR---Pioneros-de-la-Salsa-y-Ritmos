package com.jhanp.psraerobicsbackend.modules.aerobics.payment.enums;

/**
 * Las constantes están en inglés, pero los valores de tipo_movimiento_enum siguen en español: el
 * esquema no se toca. La traducción la hace {@link MovementTypeConverter}, y hacia los
 * procedimientos almacenados hay que pasar {@link #getDbValue()}, nunca name().
 */
public enum MovementType {
    PAYMENT("ABONO"),
    REFUND("REEMBOLSO");

    private final String dbValue;

    MovementType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static MovementType fromDbValue(String dbValue) {
        for (MovementType value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para tipo_movimiento_enum: " + dbValue);
    }
}
