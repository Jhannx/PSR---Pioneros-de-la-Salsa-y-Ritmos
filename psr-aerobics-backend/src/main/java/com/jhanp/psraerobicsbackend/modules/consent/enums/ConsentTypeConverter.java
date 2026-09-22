package com.jhanp.psraerobicsbackend.modules.consent.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de consentimiento.tipo.
 * <p>
 * Mismo patrón que RegistrationSourceConverter, con una diferencia que importa: la columna de
 * destino es VARCHAR con CHECK y no un tipo ENUM de Postgres, precisamente para que este converter
 * funcione también al INSERTAR. Ver el comentario de la tabla consentimiento en V1__INITIAL_DDL.sql.
 */
@Converter(autoApply = true)
public class ConsentTypeConverter implements AttributeConverter<ConsentType, String> {

    @Override
    public String convertToDatabaseColumn(ConsentType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ConsentType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ConsentType.fromDbValue(dbData);
    }
}
