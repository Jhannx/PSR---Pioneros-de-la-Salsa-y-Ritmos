package com.jhanp.psraerobicsbackend.modules.role.enums;

/**
 * Las constantes están en inglés, pero los valores de rol_nombre_enum siguen en español: el esquema
 * no se toca. La traducción en las dos direcciones la hace {@link RoleNameConverter}, y hacia los
 * procedimientos almacenados hay que pasar {@link #getDbValue()}, nunca name().
 */
public enum RoleName {
    ADMIN("ADMIN"),
    MEMBER("MIEMBRO"),
    TEACHER("PROFESOR");

    private final String dbValue;

    RoleName(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static RoleName fromDbValue(String dbValue) {
        for (RoleName value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para rol_nombre_enum: " + dbValue);
    }
}
