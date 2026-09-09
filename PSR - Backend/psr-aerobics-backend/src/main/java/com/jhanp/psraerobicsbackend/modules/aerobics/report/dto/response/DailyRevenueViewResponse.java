package com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Salida de GET /api/reportes/recaudo-diario, desde vista_recaudo_diario: total recaudado por día y
 * sesión. Los pagos por adelantado, que no cuelgan de ninguna llegada, aparecen con sessionId nulo.
 * <p>
 * No expone el id de la vista: es una clave sintética de ROW_NUMBER(), no es estable entre consultas
 * y solo existe para que Hibernate pueda identificar la fila.
 */
public record DailyRevenueViewResponse(
        LocalDate date,
        Integer sessionId,
        BigDecimal totalCollected
) {
}
