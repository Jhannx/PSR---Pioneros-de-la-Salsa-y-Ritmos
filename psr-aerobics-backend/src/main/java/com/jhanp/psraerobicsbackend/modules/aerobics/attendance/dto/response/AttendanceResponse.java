package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Salida de PUT /api/llegadas/{id}/asistencia.
 * <p>
 * Ninguno de estos campos viaja en la petición: el id de la llegada va en la URL, el usuario que
 * marca sale del JWT y el monto lo cobra fn_tarifa_vigente() dentro de sp_registrar_asistencia.
 * Por eso el service tiene que releer la fila de asistencia después del CALL (patrón H).
 */
public record AttendanceResponse(
        Integer attendanceId,
        Integer checkinId,
        Integer personId,
        String fullName,
        BigDecimal amountCharged,
        Integer markedByUserId,
        String markedByName,
        OffsetDateTime markedAt
) {
}
