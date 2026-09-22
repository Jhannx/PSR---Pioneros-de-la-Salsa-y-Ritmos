package com.jhanp.psraerobicsbackend.modules.aerobics.report.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DailyRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DashboardResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.SessionRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /**
     * Métricas del dashboard, ya calculadas: el front solo las pinta.
     * <p>
     * Los ingresos son devengados —atribuidos a la fecha de la clase, no a la del pago—, a diferencia
     * de los métodos de recaudo de arriba, que responden "cuánto entró en caja".
     *
     * @param date  el día del que se quieren los totales
     * @param from  inicio de la ventana de los promedios; null → los 30 días que terminan en date
     * @param to    fin de la ventana; null → date
     */
    DashboardResponse getDashboard(LocalDate date, LocalDate from, LocalDate to);

    Page<PersonBalanceViewResponse> getPersonBalances(Pageable pageable);

    /**
     * Lo abonado hoy, neto de reembolsos: la caja del dia. Distinto del ingreso devengado del
     * dashboard, que atribuye el dinero a la fecha de la clase y no a la del pago.
     */
    java.math.BigDecimal getCollectedToday();
}
