package com.jhanp.psraerobicsbackend.modules.aerobics.report.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DailyRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.SessionRevenueViewResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Reportes de recaudo. El módulo no tiene entidad de tabla propia: vive por completo de vistas.
 */
public interface IReportService {

    /**
     * Recaudo por día y sesión. Los pagos por adelantado, que no cuelgan de ninguna llegada,
     * aparecen con sessionId nulo. Un día tiene una o dos sesiones, así que no se pagina.
     */
    List<DailyRevenueViewResponse> getDailyRevenue(LocalDate date);

    List<DailyRevenueViewResponse> getDailyRevenueBetween(LocalDate from, LocalDate to);

    /**
     * Recaudo por sesión: solo cuenta movimientos ligados a una llegada.
     */
    Optional<SessionRevenueViewResponse> getSessionRevenue(Integer sessionId);

    List<SessionRevenueViewResponse> getSessionRevenueBetween(LocalDate from, LocalDate to);
}
