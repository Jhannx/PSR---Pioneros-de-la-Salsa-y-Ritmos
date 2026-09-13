package com.jhanp.psraerobicsbackend.modules.role.enums;

/** Enum de roles de seguridad y sus valores persistidos. */
public enum RoleName {
    ADMIN("ADMIN"),
    MEMBER("MIEMBRO"),
    TEACHER("PROFESOR"),
    RECEPTIONIST("RECEPCIONISTA"),
    PSR_TERMINAL("PSR_TERMINAL");

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