package com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Salida de GET /api/sesiones/resumen y /api/sesiones/{id}/resumen, desde vista_resumen_sesiones:
 * el profesor ya resuelto y los totales de llegadas y atendidos ya contados.
 * <p>
 * teacherName llega como "Sin asignar" cuando la sesión no tiene profesor.
 */
public record SessionSummaryViewResponse(
        Integer sessionId,
        Integer scheduleId,
        LocalDate date,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer teacherUserId,
        String teacherName,
        Boolean cancelled,
        Long totalCheckins,
        Long totalAttended
) {
}
