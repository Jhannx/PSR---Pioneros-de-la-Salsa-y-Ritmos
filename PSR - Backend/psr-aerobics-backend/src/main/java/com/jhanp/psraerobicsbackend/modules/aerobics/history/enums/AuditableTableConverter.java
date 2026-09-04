package com.jhanp.psraerobicsbackend.modules.aerobics.history.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de tabla_auditable_enum.
 */
@Converter(autoApply = true)
public class AuditableTableConverter implements AttributeConverter<AuditableTable, String> {

    @Override
    public String convertToDatabaseColumn(AuditableTable attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public AuditableTable convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AuditableTable.fromDbValue(dbData);
    }
}
