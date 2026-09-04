package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de metodo_checkin_enum.
 */
@Converter(autoApply = true)
public class CheckinMethodConverter implements AttributeConverter<CheckinMethod, String> {

    @Override
    public String convertToDatabaseColumn(CheckinMethod attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public CheckinMethod convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CheckinMethod.fromDbValue(dbData);
    }
}
