package com.jhanp.psraerobicsbackend.modules.aerobics.report.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DailyRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.SessionRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.service.interfaces.IReportService;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final IReportService reportService;

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
