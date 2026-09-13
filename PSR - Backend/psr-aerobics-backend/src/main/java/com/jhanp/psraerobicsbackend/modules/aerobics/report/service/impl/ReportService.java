package com.jhanp.psraerobicsbackend.modules.aerobics.report.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DailyRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DashboardResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.SessionRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.mapper.RevenueViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.repository.DailyRevenueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.repository.DashboardRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.repository.SessionRevenueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.service.interfaces.IReportService;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.mapper.PersonViewMapper;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonBalanceViewRepository;
import com.jhanp.psraerobicsbackend.shared.validation.DateRanges;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Solo lectura sobre las dos vistas de recaudo. El módulo no tiene entidad de tabla propia.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService implements IReportService {

    /** Ventana por defecto de los promedios del dashboard, contando el propio día consultado. */
    private static final int DEFAULT_WINDOW_DAYS = 30;

    private final DailyRevenueViewRepository dailyRevenueViewRepository;
    private final SessionRevenueViewRepository sessionRevenueViewRepository;
    private final DashboardRepository dashboardRepository;
    private final RevenueViewMapper revenueViewMapper;
    private final PersonBalanceViewRepository personBalanceViewRepository;
    private final PersonViewMapper personViewMapper;

    @Override
    public List<DailyRevenueViewResponse> getDailyRevenue(LocalDate date) {
        log.info("Consultando el recaudo del día: {}", date);
        return dailyRevenueViewRepository.findByDateOrderBySessionIdAsc(date).stream()
                .map(revenueViewMapper::toResponse)
                .toList();
    }

    @Override
    public List<DailyRevenueViewResponse> getDailyRevenueBetween(LocalDate from, LocalDate to) {
        log.info("Consultando el recaudo diario entre {} y {}", from, to);
        DateRanges.require(from, to);
        return dailyRevenueViewRepository.findByDateBetweenOrderByDateDesc(from, to).stream()
                .map(revenueViewMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<SessionRevenueViewResponse> getSessionRevenue(Integer sessionId) {
        log.info("Consultando el recaudo de la sesión ID: {}", sessionId);
        return sessionRevenueViewRepository.findBySessionId(sessionId)
                .map(revenueViewMapper::toResponse);
    }

    @Override
    public List<SessionRevenueViewResponse> getSessionRevenueBetween(LocalDate from, LocalDate to) {
        log.info("Consultando el recaudo por sesión entre {} y {}", from, to);
        DateRanges.require(from, to);
        return sessionRevenueViewRepository.findByDateBetweenOrderByDateDesc(from, to).stream()
                .map(revenueViewMapper::toResponse)
                .toList();
    }

    /**
     * Siete consultas: los cuatro periodos de calendario, la ventana de los promedios, el gráfico
     * semanal y el método de pago. Son agregaciones sobre rangos distintos de la misma vista, así que
     * juntarlas en una sola sentencia daría un SQL bastante peor de leer sin ahorrar nada apreciable.
     * <p>
     * Se calculan los cuatro periodos aunque el front muestre uno solo: es lo que permite que el filtro
     * de día/semana/mes/año cambie al instante, sin otra ida al servidor por cada toque.
     */
    @Override
    public DashboardResponse getDashboard(LocalDate date, LocalDate from, LocalDate to) {
        LocalDate day = date != null ? date : LocalDate.now();

        // Ventana móvil de 30 días terminando en el día consultado. minusDays(29) y no 30: el rango es
        // inclusivo en ambos extremos, así que 29 atrás más el propio día son 30.
        LocalDate windowTo = to != null ? to : day;
        LocalDate windowFrom = from != null ? from : windowTo.minusDays(DEFAULT_WINDOW_DAYS - 1);
        DateRanges.require(windowFrom, windowTo);

        log.info("Dashboard del {} con ventana {} a {}", day, windowFrom, windowTo);

        DashboardRepository.RangeTotals window = dashboardRepository.findRangeTotals(windowFrom, windowTo);
        long daysWithSession = window.daysWithSession();

        return new DashboardResponse(
                buildPeriodTotals(day),
                averageAttendees(window.attendees(), daysWithSession),
                averageRevenue(window.revenue(), daysWithSession),
                dashboardRepository.findMainPaymentMethod(),
                buildWeeklyAverage(windowFrom, windowTo));
    }

    /**
     * Saldo de todas las personas activas, paginado. Vive en reportes y no en persona porque es una
     * lectura agregada del dinero, igual que el resto de este módulo.
     * <p>
     * Existe para que la pestaña de cuentas no tenga que pedir el saldo persona a persona: con el
     * endpoint por persona, pintar una lista de doscientas serían doscientas peticiones.
     * <p>
     * Deudores primero, que es el orden de la vista: son los que hay que mirar.
     */
    @Override
    public Page<PersonBalanceViewResponse> getPersonBalances(Pageable pageable) {
        return personBalanceViewRepository.findAllByOrderByBalanceAsc(pageable)
                .map(personViewMapper::toResponse);
    }

    /**
     * Los cuatro periodos de CALENDARIO que contienen el día consultado, no ventanas móviles: la
     * semana es de lunes a domingo, el mes del 1 al último día, el año del 1 de enero al 31 de
     * diciembre. Es lo que se entiende por "cuánto llevamos este mes".
     * <p>
     * Se calculan los cuatro siempre, aunque el front solo muestre uno: son agregaciones baratas
     * sobre la misma vista y así el filtro cambia sin otra ida al servidor.
     * <p>
     * Que el rango incluya días futuros —un 3 de marzo, el mes llega hasta el 31— es inofensivo:
     * todavía no hay filas ahí.
     */
    private DashboardResponse.PeriodTotals buildPeriodTotals(LocalDate day) {
        return new DashboardResponse.PeriodTotals(
                totalsBetween(day, day),
                totalsBetween(
                        day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                        day.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))),
                totalsBetween(
                        day.withDayOfMonth(1),
                        day.with(TemporalAdjusters.lastDayOfMonth())),
                totalsBetween(
                        day.withDayOfYear(1),
                        day.with(TemporalAdjusters.lastDayOfYear())));
    }

    private DashboardResponse.Totals totalsBetween(LocalDate from, LocalDate to) {
        DashboardRepository.RangeTotals totals = dashboardRepository.findRangeTotals(from, to);
        return new DashboardResponse.Totals(totals.attendees(), totals.revenue());
    }

    private long averageAttendees(long total, long daysWithSession) {
        return daysWithSession == 0 ? 0 : Math.round((double) total / daysWithSession);
    }

    /**
     * Se redondea a PESOS ENTEROS, sin centavos.
     * <p>
     * En pesos colombianos el centavo no circula: los montos reales son 4.000, 172.000. Esta división
     * es el único punto de todo el sistema donde el dinero podría fraccionarse —el resto son sumas,
     * restas y comparaciones, que preservan enteros—, así que es el único sitio donde hay que cortarlo.
     * Con escala 2 el dashboard mostraría promedios como 172.333,33, que no significan nada.
     * <p>
     * Sin días de clase el promedio no es cero sino indefinido; se devuelve cero porque el dashboard
     * tiene que pintar algo, pero dividir habría lanzado ArithmeticException.
     */
    private BigDecimal averageRevenue(BigDecimal total, long daysWithSession) {
        if (daysWithSession == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(daysWithSession), 0, RoundingMode.HALF_UP);
    }

    /**
     * Los siete días siempre, en orden de lunes a domingo, aunque el gimnasio no abra alguno: el
     * gráfico dibuja un eje completo y no debería tener que rellenar huecos. Un día sin clases en la
     * ventana sale en cero, y empezará a mostrar su número solo si algún día abren.
     */
    private List<DashboardResponse.WeeklyAttendanceAverage> buildWeeklyAverage(LocalDate from, LocalDate to) {
        Map<Integer, Long> averageByDay = dashboardRepository.findWeeklyAttendanceAverage(from, to);

        return IntStream.rangeClosed(1, 7)
                .mapToObj(day -> new DashboardResponse.WeeklyAttendanceAverage(
                        day, averageByDay.getOrDefault(day, 0L)))
                .toList();
    }

}
