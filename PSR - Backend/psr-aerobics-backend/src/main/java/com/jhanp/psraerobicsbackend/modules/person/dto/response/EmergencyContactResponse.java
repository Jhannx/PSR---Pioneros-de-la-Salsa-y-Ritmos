package com.jhanp.psraerobicsbackend.modules.person.dto.response;

import java.time.OffsetDateTime;

/**
 * Salida de GET/PUT /api/personas/{id}/contacto-emergencia.
 * <p>
 * Sin id propio: la relación con persona es 1:1 (persona_id es UNIQUE), así que personId ya
 * identifica la fila y es con lo que el frontend la direcciona.
 */
public record EmergencyContactResponse(
        Integer personId,
        String name,
        String phone,
        String observations,
        OffsetDateTime updatedAt
) {
}
