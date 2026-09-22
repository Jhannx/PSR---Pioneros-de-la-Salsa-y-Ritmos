package com.jhanp.psraerobicsbackend.modules.consent.enums;

/**
 * Los cuatro consentimientos que la política de tratamiento de datos distingue, y que el frontend ya
 * pide como cuatro casillas separadas.
 * <p>
 * Que sean cuatro y no uno no es burocracia: el artículo 5 de la Ley 1581 obliga a que el de salud
 * sea <b>diferenciado y expreso</b>, y meterlo dentro de la aceptación general es exactamente lo que
 * prohíbe. Los otros dos se separan por la misma razón de fondo — se pueden dar unos y no otros, y
 * revocar uno no revoca los demás.
 *
 * <pre>
 *   DATA_PROCESSING          numeral 4.1  autorización general de tratamiento
 *   VOLUNTARY_PARTICIPATION  numeral 12   "participo de manera voluntaria"
 *   HEALTH_DATA              numeral 4.2  dato sensible, opcional, no condiciona el servicio
 *   THIRD_PARTY_DATA         numeral 5    declara estar autorizado a dar los datos del contacto
 * </pre>
 * <p>
 * Las constantes están en inglés y los valores guardados en español, como todo el esquema. La
 * traducción la hace {@link ConsentTypeConverter}.
 */
public enum ConsentType {

    /**
     * Numeral 4.1. Es el único que bloquea el alta: registrar a alguien <em>es</em> tratar sus datos,
     * y sin autorización no hay base para hacerlo.
     */
    DATA_PROCESSING("TRATAMIENTO_DATOS"),

    /**
     * Numeral 12. No es tratamiento de datos —la propia política lo declara independiente— sino la
     * constancia de que la persona entiende que la actividad física conlleva riesgo de lesión. Se
     * guarda en la misma tabla porque el problema es el mismo: hay que poder probar que lo aceptó.
     */
    VOLUNTARY_PARTICIPATION("PARTICIPACION_VOLUNTARIA"),

    /**
     * Numeral 4.2. Dato sensible. <b>No puede bloquear nada</b>: la ley prohíbe condicionar el
     * servicio a que la persona entregue datos de salud. Lo único que bloquea es guardar el dato en
     * sí — sin esta autorización, observaciones_salud no se escribe.
     */
    HEALTH_DATA("DATOS_SALUD"),

    /**
     * Numeral 5. El contacto de emergencia es una persona que nunca habló con el sistema y que
     * probablemente no sabe que sus datos están aquí. Lo que se guarda no es su consentimiento —no se
     * lo podemos pedir— sino la declaración de quien los entrega de que está autorizado a hacerlo.
     */
    THIRD_PARTY_DATA("DATOS_TERCEROS");

    private final String dbValue;

    ConsentType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static ConsentType fromDbValue(String dbValue) {
        for (ConsentType value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para consentimiento.tipo: " + dbValue);
    }
}
