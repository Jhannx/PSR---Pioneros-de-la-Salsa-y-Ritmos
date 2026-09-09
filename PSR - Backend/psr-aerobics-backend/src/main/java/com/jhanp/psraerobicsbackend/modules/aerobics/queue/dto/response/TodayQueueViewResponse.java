package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Salida de GET /api/cola/admin, desde vista_fila_hoy. Pantalla de administración: sí lleva dinero.
 * <p>
 * attendanceId, amountCharged y markedAt vienen en null mientras la asistencia no se confirme.
 * position la calcula la vista por sesión.
 */
public record TodayQueueViewResponse(
        Integer checkinId,
        Integer sessionId,
        Integer personId,
        String firstName,
        String lastName,
        OffsetDateTime arrivalTime,
        CheckinMethod checkinMethod,
        Long position,
        Integer attendanceId,
        BigDecimal amountCharged,
        OffsetDateTime markedAt,
        Boolean attended,
        BigDecimal balance,
        String note
) {
}
