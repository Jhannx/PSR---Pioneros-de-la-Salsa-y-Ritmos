package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Entrada de POST /api/checkin/qr y POST /api/checkin/manual.
 * <p>
 * Un solo campo a propósito:
 * <ul>
 *   <li>sessionId no va: la resuelve fn_obtener_o_crear_sesion_del_momento() dentro del
 *       procedimiento. El cliente nunca elige sesión.</li>
 *   <li>checkinMethod tampoco: lo fija la ruta (QR o MANUAL), no el body. Si viniera del cliente,
 *       un check-in manual podría registrarse como si hubiera sido por QR.</li>
 * </ul>
 */
public record CheckinRequest(
        @NotNull(message = "El ID de persona es obligatorio")
        @Positive(message = "El ID de persona debe ser positivo")
        Integer personId
) {
}
