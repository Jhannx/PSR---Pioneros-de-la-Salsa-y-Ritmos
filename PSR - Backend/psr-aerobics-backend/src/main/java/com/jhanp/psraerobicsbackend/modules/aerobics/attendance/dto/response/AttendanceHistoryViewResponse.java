package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Salida de GET /api/asistencias/historial, desde vista_historial_asistencias.
 * <p>
 * La vista ya resuelve persona, quién marcó y método de pago, así que no hay que rehacer esos JOIN.
 * fullName llega como "Persona eliminada" si la persona fue purgada, y paymentMethod es null si el
 * cobro no tiene movimiento ligado.
 */
public record AttendanceHistoryViewResponse(
        Integer attendanceId,
        Integer checkinId,
        Integer sessionId,
        Integer personId,
        String fullName,
        LocalDate date,
        OffsetDateTime arrivalTime,
        CheckinMethod checkinMethod,
        BigDecimal amountCharged,
        OffsetDateTime markedAt,
        Integer markedByUserId,
        String markedByName,
        String paymentMethod
) {
}
