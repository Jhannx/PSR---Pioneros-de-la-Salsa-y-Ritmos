package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;

import java.time.OffsetDateTime;

/**
 * Salida común de los tres endpoints de check-in (/qr, /documento, /manual).
 * <p>
 * sp_registrar_llegada solo devuelve p_llegada_id por OUT: el nombre, la sesión y la posición en la
 * cola salen de una lectura posterior a vista_fila_hoy dentro de la misma transacción (patrón H).
 * <p>
 * queuePosition viene en null en las lecturas sueltas de la tabla llegada, que no calculan posición.
 */
public record CheckinResponse(
        Integer checkinId,
        Integer personId,
        Integer sessionId,
        String fullName,
        OffsetDateTime arrivalTime,
        CheckinMethod checkinMethod,
        Long queuePosition
) {
}
