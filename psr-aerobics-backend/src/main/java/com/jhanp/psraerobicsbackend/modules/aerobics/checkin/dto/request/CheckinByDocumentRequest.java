package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /api/checkin/documento. En el kiosko la persona digita su documento porque no
 * conoce su id interno; si no existe, el endpoint responde 404 y el kiosko ofrece registrarla.
 * <p>
 * DTO dedicado y no reutilización de CheckinRequest: el solapamiento con él es 0 % — identifican a
 * la persona por campos distintos.
 */
public record CheckinByDocumentRequest(
        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres")
        String documentNumber
) {
}
