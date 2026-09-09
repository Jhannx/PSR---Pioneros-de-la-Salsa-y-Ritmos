package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.service.interfaces.IAttendanceService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Gestión y consulta de asistencias a clases.
 */
@RestController
@RequiredArgsConstructor
@Validated
public class AttendanceController {

    private final IAttendanceService attendanceService;

    /**
     * Confirma la asistencia de una llegada registrada (sin body).
     * <p>
     * El usuario que confirma la asistencia sale del contexto de autenticación (JWT).
     */
    @PutMapping("/api/llegadas/{id}/asistencia")
    public ResponseEntity<AttendanceResponse> confirm(
            @PathVariable @Positive(message = "El ID de llegada debe ser positivo") Integer id,
            Authentication authentication) {

        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(attendanceService.confirm(id, userId));
    }

    /**
     * Anula una asistencia confirmada previamente.
     */
    @DeleteMapping("/api/llegadas/{id}/asistencia")
    public ResponseEntity<Void> cancel(
            @PathVariable @Positive(message = "El ID de llegada debe ser positivo") Integer id) {

        attendanceService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Consulta el registro de asistencia ligado a una llegada.
     */
    @GetMapping("/api/llegadas/{id}/asistencia")
    public ResponseEntity<AttendanceResponse> findByCheckinId(
            @PathVariable @Positive(message = "El ID de llegada debe ser positivo") Integer id) {

        return ResponseEntity.ok(attendanceService.findByCheckinId(id));
    }

    /**
     * Historial de asistencias de una persona específica.
     */
    @GetMapping("/api/asistencias/historial/persona/{personaId}")
    public ResponseEntity<Page<AttendanceHistoryViewResponse>> getHistoryByPerson(
            @PathVariable("personaId") @Positive(message = "El ID de persona debe ser positivo") Integer personId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(attendanceService.getHistoryByPerson(personId, pageable));
    }

    /**
     * Historial de asistencias de una sesión específica.
     */
    @GetMapping("/api/asistencias/historial/sesion/{sesionId}")
    public ResponseEntity<Page<AttendanceHistoryViewResponse>> getHistoryBySession(
            @PathVariable("sesionId") @Positive(message = "El ID de sesión debe ser positivo") Integer sessionId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(attendanceService.getHistoryBySession(sessionId, pageable));
    }

    /**
     * Historial de asistencias en un rango de fechas.
     */
    @GetMapping("/api/asistencias/historial")
    public ResponseEntity<Page<AttendanceHistoryViewResponse>> getHistoryBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(attendanceService.getHistoryBetween(desde, hasta, pageable));
    }

    /**
     * Cobertura de saldo asistencia por asistencia de una persona.
     */
    @GetMapping("/api/personas/{id}/saldo-asistencias")
    public ResponseEntity<Page<AttendanceBalanceViewResponse>> getBalanceByPerson(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(attendanceService.getBalanceByPerson(id, pageable));
    }

    private Integer resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Integer uid) {
            return uid;
        }
        if (authentication != null) {
            try {
                return Integer.parseInt(authentication.getName());
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }
}
