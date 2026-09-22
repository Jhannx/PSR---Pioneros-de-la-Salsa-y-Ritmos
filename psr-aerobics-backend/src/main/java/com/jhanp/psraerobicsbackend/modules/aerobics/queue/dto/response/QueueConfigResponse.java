package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response;

import java.time.OffsetDateTime;

/**
 * Salida de GET/PUT /api/cola/configuracion. Sin id: configuracion_cola es una tabla de fila única
 * (siempre id = 1), así que exponerlo solo invita al cliente a creer que puede elegir otra fila.
 * <p>
 * El backend expone el valor de peoplePerTurn; qué filas se resaltan como "les toca pasar" es
 * lógica exclusiva del frontend de la pantalla de espera.
 */
public record QueueConfigResponse(
        Integer peoplePerTurn,
        Integer entryMarginMinutes,
        Integer paymentVisibilityMinutes,
        OffsetDateTime updatedAt,
        Integer updatedByUserId
) {
}
