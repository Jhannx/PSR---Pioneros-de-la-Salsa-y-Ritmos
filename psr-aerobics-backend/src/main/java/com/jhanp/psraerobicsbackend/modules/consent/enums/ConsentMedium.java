package com.jhanp.psraerobicsbackend.modules.consent.enums;

/**
 * Cómo se recogió el consentimiento.
 * <p>
 * Existe porque el sistema tiene dos puertas de alta que funcionan distinto: el autoregistro digital
 * (kiosco y web) muestra las casillas en pantalla, y el alta manual de recepción se hace con un
 * formato en papel firmado. Si el modelo solo admitiera lo digital, la mitad de las altas no tendría
 * forma de dejar constancia y la tabla mentiría por omisión.
 */
public enum ConsentMedium {

    /** Casilla marcada en pantalla por la propia persona. */
    DIGITAL("DIGITAL"),

    /**
     * Formato impreso firmado, archivado fuera del sistema. La fila dice que existe; dónde se guarda
     * el papel y por cuánto tiempo es una decisión operativa aún abierta (ver
     * {@code docs/pendiente-implementar-sistema.md}).
     */
    PHYSICAL("FISICO");

    private final String dbValue;

    ConsentMedium(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static ConsentMedium fromDbValue(String dbValue) {
        for (ConsentMedium value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para consentimiento.medio: " + dbValue);
    }
}
