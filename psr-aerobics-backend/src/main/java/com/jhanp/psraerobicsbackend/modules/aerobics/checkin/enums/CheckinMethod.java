package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums;

/**
 * Las constantes están en inglés, pero los valores de metodo_checkin_enum siguen en español: el
 * esquema no se toca. La traducción la hace {@link CheckinMethodConverter}, y hacia los
 * procedimientos almacenados hay que pasar {@link #getDbValue()}, nunca name().
 */
public enum CheckinMethod {
    QR("QR"),
    DOCUMENT("DOCUMENTO"),
    MANUAL("MANUAL"),

    /**
     * La llegada que marca la propia persona desde su pantalla.
     * <p>
     * Antes este caso se guardaba como {@link #DOCUMENT}, reutilizando el valor de la terminal del
     * lobby para no tocar el esquema. Se vio en la prueba del 20-sep: el check-in de recepcion decia
     * "Terminal PSR" de alguien que habia marcado su llegada desde el telefono. El dato guardado era
     * el que mentia, no la pantalla.
     * <p>
     * <b>Y no QR ni APP.</b> El QR de la sala abre esta misma pantalla, asi que las dos cosas llegan
     * por el mismo endpoint con el mismo token: el servidor no puede distinguirlas, y un nombre que
     * afirmara cual fue estaria inventando. Lo unico que consta es que lo marco la persona.
     */
    SELF("PROPIO");

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
