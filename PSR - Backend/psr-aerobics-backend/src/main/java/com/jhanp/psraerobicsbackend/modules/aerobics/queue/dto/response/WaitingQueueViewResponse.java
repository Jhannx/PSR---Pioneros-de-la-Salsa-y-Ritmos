package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response;

import java.time.OffsetDateTime;

/**
 * Salida de GET /api/cola/espera, desde vista_fila_espera. Pantalla de la sala de espera: cero datos
 * de dinero (RNF-03).
 * <p>
 * Comparte ~45 % de los campos con {@link TodayQueueViewResponse} y aun así es un DTO dedicado a
 * propósito: la separación es una barrera de privacidad, no una duplicación. Así la pantalla de TV
 * no puede exponer saldo ni monto aunque alguien se equivoque en el controller.
 */
public record WaitingQueueViewResponse(
        Integer checkinId,
        Integer sessionId,
        String firstName,
        String lastName,
        OffsetDateTime arrivalTime,
        Long position
) {
}
