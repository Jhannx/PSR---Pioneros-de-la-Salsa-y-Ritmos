package com.jhanp.psraerobicsbackend.modules.person.dto.response;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;

import java.time.OffsetDateTime;

/**
 * Salida de los endpoints de persona.
 * <p>
 * sp_registrar_persona solo devuelve el id por OUT: source, registeredAt y active salen de releer la
 * fila después del CALL (patrón H). userId es la cuenta creada en la misma transacción — no cuelga
 * de la entidad Person, la aporta el service.
 */
public record PersonResponse(
        Integer id,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String phone,
        RegistrationSource source,
        OffsetDateTime registeredAt,
        Boolean active,
        Integer userId
) {
}
