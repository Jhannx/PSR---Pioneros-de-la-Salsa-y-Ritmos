package com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Salida de GET /api/reportes/recaudo-por-sesion, desde vista_recaudo_por_sesion: el recaudo de cada
 * sesión, para los días con más de una. Solo cuenta movimientos ligados a una llegada.
 * <p>
 * Misma forma que {@link DailyRevenueViewResponse} — son dos agregaciones distintas en SQL sobre un
 * mismo contrato JSON. Se mantienen separados porque cada endpoint documenta su propia semántica.
 */
public record SessionRevenueViewResponse(
        LocalDate date,
        Integer sessionId,
        BigDecimal totalCollected
) {
}
