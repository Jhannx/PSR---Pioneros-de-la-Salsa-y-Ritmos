package com.jhanp.psraerobicsbackend.modules.aerobics.payment.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de tipo_movimiento_enum.
 */
@Converter(autoApply = true)
public class MovementTypeConverter implements AttributeConverter<MovementType, String> {

    @Override
    public String convertToDatabaseColumn(MovementType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public MovementType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MovementType.fromDbValue(dbData);
    }
}
