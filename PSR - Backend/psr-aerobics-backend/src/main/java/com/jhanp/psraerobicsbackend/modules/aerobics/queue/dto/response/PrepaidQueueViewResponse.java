package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response;

import java.time.OffsetDateTime;

/**
 * Salida de GET /api/cola/prepagada, desde vista_fila_prepagada: quienes ya pagaron antes de llegar
 * y pueden entrar directo. Sin dinero ni posición.
 * <p>
 * La vista aplica sola la ventana de minutos_visibilidad_pagos, así que una fila desaparece de aquí
 * al vencer ese tiempo. Solo cuenta el pago hecho antes de la llegada — no es "tiene saldo ahora".
 */
public record PrepaidQueueViewResponse(
        Integer checkinId,
        String firstName,
        String lastName,
        OffsetDateTime arrivalTime
) {
}
