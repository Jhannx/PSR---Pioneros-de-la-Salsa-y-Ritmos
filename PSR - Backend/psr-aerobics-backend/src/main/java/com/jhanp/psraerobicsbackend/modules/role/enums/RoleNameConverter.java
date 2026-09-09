package com.jhanp.psraerobicsbackend.modules.role.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de rol_nombre_enum. Reemplaza a
 * @Enumerated(EnumType.STRING), que mapea por el nombre de la constante y guardaría 'MEMBER'.
 */
@Converter(autoApply = true)
public class RoleNameConverter implements AttributeConverter<RoleName, String> {

    @Override
    public String convertToDatabaseColumn(RoleName attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public RoleName convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RoleName.fromDbValue(dbData);
    }
}
