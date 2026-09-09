package com.jhanp.psraerobicsbackend.modules.aerobics.history.enums;

/**
 * Las constantes están en inglés, pero los valores de tabla_auditable_enum siguen en español: son
 * los nombres reales de las tablas y los escriben los triggers, así que el esquema no se toca. La
 * traducción la hace {@link AuditableTableConverter}; para consultar por el valor crudo hay que
 * pasar {@link #getDbValue()}, nunca name().
 */
public enum AuditableTable {
    ATTENDANCE("ASISTENCIA"),
    ACCOUNT_MOVEMENT("MOVIMIENTO_CUENTA"),
    PRICING("TARIFA"),
    QUEUE_CONFIG("CONFIGURACION_COLA"),
    PERSON("PERSONA"),
    SESSION("SESION");

    private final String dbValue;

    AuditableTable(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static AuditableTable fromDbValue(String dbValue) {
        for (AuditableTable value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para tabla_auditable_enum: " + dbValue);
    }
}
