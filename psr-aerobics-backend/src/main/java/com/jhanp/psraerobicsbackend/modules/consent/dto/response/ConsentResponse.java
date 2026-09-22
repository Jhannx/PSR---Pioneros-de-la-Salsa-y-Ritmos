package com.jhanp.psraerobicsbackend.modules.consent.dto.response;

import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;

import java.time.OffsetDateTime;

/**
 * Una fila del registro de consentimiento, tal cual se guardó.
 * <p>
 * Se expone entera —incluidos medio y versión— porque este endpoint es la herramienta con la que se
 * responde a "demuéstreme que esta persona autorizó": una respuesta que solo dijera "sí" no serviría
 * para eso.
 */
public record ConsentResponse(
        Long id,
        Integer personId,
        ConsentType type,
        Boolean granted,
        String policyVersion,
        ConsentMedium medium,
        OffsetDateTime recordedAt,

        /** null cuando no había sesión (autoregistro); con valor, la cuenta que lo afirmó. */
        Integer registeredByUserId
) {
}
