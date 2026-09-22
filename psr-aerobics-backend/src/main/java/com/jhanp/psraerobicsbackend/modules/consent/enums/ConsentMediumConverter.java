package com.jhanp.psraerobicsbackend.modules.consent.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de consentimiento.medio.
 * <p>
 * DIGITAL coincide en los dos idiomas y PHYSICAL no (FISICO). El converter se aplica igual a los dos:
 * tratar uno a mano y el otro por convención es cómo se acaban colando valores que la columna rechaza.
 */
@Converter(autoApply = true)
public class ConsentMediumConverter implements AttributeConverter<ConsentMedium, String> {

    @Override
    public String convertToDatabaseColumn(ConsentMedium attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public ConsentMedium convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ConsentMedium.fromDbValue(dbData);
    }
}
