package com.jhanp.psraerobicsbackend.modules.person.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de fuente_registro_enum.
 */
@Converter(autoApply = true)
public class RegistrationSourceConverter implements AttributeConverter<RegistrationSource, String> {

    @Override
    public String convertToDatabaseColumn(RegistrationSource attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public RegistrationSource convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RegistrationSource.fromDbValue(dbData);
    }
}
