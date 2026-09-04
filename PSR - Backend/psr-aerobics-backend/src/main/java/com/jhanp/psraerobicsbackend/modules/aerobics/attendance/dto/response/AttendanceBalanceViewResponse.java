package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Salida de GET /api/personas/{id}/saldo-asistencias, desde vista_saldo_por_asistencia: para cada
 * asistencia cobrada dice si el saldo acumulado la cubría.
 * <p>
 * balanceStatus queda como String y no como enum: sus valores (CUBIERTA, PARCIAL, DEBE) los produce
 * un CASE de la vista, no un tipo de Postgres, así que no hay enum del cual derivarlo.
 */
public record AttendanceBalanceViewResponse(
        Integer attendanceId,
        Integer personId,
        LocalDate date,
        BigDecimal amountCharged,
        BigDecimal balanceAfterThisAttendance,
        String balanceStatus,
        BigDecimal pendingAmount
) {
}
