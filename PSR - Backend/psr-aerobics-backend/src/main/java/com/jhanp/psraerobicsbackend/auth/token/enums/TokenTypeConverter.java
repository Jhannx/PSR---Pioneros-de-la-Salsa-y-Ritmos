package com.jhanp.psraerobicsbackend.auth.token.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce entre las constantes en inglés y los valores en español de token_recuperacion.tipo.
 * Reemplaza a @Enumerated(EnumType.STRING), que mapea por el nombre de la constante y guardaría
 * 'INVITATION'.
 */
@Converter(autoApply = true)
public class TokenTypeConverter implements AttributeConverter<TokenType, String> {

    @Override
    public String convertToDatabaseColumn(TokenType attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public TokenType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TokenType.fromDbValue(dbData);
    }
}
