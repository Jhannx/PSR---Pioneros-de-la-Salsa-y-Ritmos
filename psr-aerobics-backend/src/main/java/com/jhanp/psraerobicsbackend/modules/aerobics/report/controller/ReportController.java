package com.jhanp.psraerobicsbackend.modules.aerobics.report.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DailyRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DashboardResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.SessionRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.CollectedTodayResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.service.interfaces.IReportService;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.jhanp.psraerobicsbackend.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reportes de recaudo. Solo lectura, todo sale de vistas.
 */
@RestController
@RequestMapping("/api/reportes")
// Recaudo: lo ve quien cobra. El profesor queda fuera, no tiene por qué ver el dinero.
@PreAuthorize(Authorities.DESK)
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final IReportService reportService;

    /**
     * Saldo de todas las personas activas, de deudor a acreedor y paginado.
     * <p>
     * Es el listado de la pestaña de cuentas. Existe para no resolverlo desde el front pidiendo el
     * saldo de una en una: eso serían tantas peticiones como personas en la página.
     */
    @GetMapping("/saldos-personas")
    public ResponseEntity<Page<PersonBalanceViewResponse>> getPersonBalances(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.getPersonBalances(pageable));
    }

    /**
     * El recaudo de hoy para el encabezado del check-in, con una definicion unica y escrita.
     * <p>
     * Es lo ABONADO hoy neto de reembolsos — la caja del dia—, no lo devengado del dashboard ni lo
     * recibido en efectivo. Un cobro de 50.000 con 45.000 de vuelto cuenta 5.000. El razonamiento
     * completo esta en {@code ReportService.getCollectedToday}.
     */
    @GetMapping("/recaudo-hoy")
    public ResponseEntity<CollectedTodayResponse> getCollectedToday() {
        return ResponseEntity.ok(new CollectedTodayResponse(reportService.getCollectedToday()));
    }

    /**
     * Métricas del dashboard, ya calculadas.
     * <p>
     * Los ingresos que devuelve son DEVENGADOS: se atribuyen a la fecha de la clase que se dictó, no a
     * la fecha en que entró el dinero. Es distinto de /recaudo-diario, que responde "cuánto entró hoy
     * en caja" — con esa otra métrica, un abono adelantado el lunes para la clase del miércoles haría
     * que el miércoles pareciera flojo.
     *
     * @param fecha del día que se quiere ver; por defecto, hoy
     * @param desde inicio de la ventana de los promedios; por defecto, 30 días atrás
     * @param hasta fin de la ventana; por defecto, el mismo día consultado
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(reportService.getDashboard(fecha, desde, hasta));
    }

    /**
     * Recaudo por día puntual y sesión.
     * <p>
     * Los pagos por adelantado, que no cuelgan de ninguna llegada, salen con sessionId nulo.
     */
    @GetMapping("/recaudo-diario/{fecha}")
    public ResponseEntity<List<DailyRevenueViewResponse>> getDailyRevenue(
            @PathVariable("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(reportService.getDailyRevenue(date));
    }

    /**
     * Recaudo diario acumulado dentro de un rango de fechas.
     */
    @GetMapping("/recaudo-diario")
    public ResponseEntity<List<DailyRevenueViewResponse>> getDailyRevenueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(reportService.getDailyRevenueBetween(desde, hasta));
    }

    /**
     * Recaudo de una sesión puntual. Solo cuenta movimientos ligados a una llegada.
     * <p>
     * Lanza ResourceNotFoundException (404) si la sesión no existe o no tiene recaudos registrados.
     */
    @GetMapping("/recaudo-por-sesion/{sesionId}")
    public ResponseEntity<SessionRevenueViewResponse> getSessionRevenue(
            @PathVariable("sesionId") @Positive(message = "El ID de sesión debe ser positivo") Integer sessionId) {

        SessionRevenueViewResponse response = reportService.getSessionRevenue(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró recaudo para la sesión con ID: " + sessionId));
        return ResponseEntity.ok(response);
    }

    /**
     * Recaudo por sesión dentro de un rango de fechas. Solo cuenta movimientos ligados a una llegada.
     */
    @GetMapping("/recaudo-por-sesion")
    public ResponseEntity<List<SessionRevenueViewResponse>> getSessionRevenueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(reportService.getSessionRevenueBetween(desde, hasta));
    }
}
