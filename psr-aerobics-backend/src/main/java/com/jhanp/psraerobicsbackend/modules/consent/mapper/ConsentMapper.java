package com.jhanp.psraerobicsbackend.modules.consent.mapper;

import com.jhanp.psraerobicsbackend.modules.consent.dto.response.ConsentResponse;
import com.jhanp.psraerobicsbackend.modules.consent.entity.Consent;
import org.mapstruct.Mapper;

/**
 * Una sola dirección, y es deliberado: no hay {@code toEntity}.
 * <p>
 * Las filas de consentimiento no se construyen desde un request — la versión de la política, la
 * fecha y quién lo registra los pone el servidor, nunca el cliente. Dejar un mapper de entrada
 * invitaría a construir la entidad desde el body y a que alguno de esos tres campos acabe viniendo
 * de fuera.
 */
@Mapper(componentModel = "spring")
public interface ConsentMapper {

    ConsentResponse toResponse(Consent consent);
}
