package com.jhanp.psraerobicsbackend.modules.aerobics.session.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.request.SessionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionSummaryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.service.interfaces.ISessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestión y consulta de sesiones de aeróbicos.
 */
@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
@Validated
public class SessionController {

    private final ISessionService sessionService;

    /**
     * Creación manual de una sesión por el administrador.
     */
    @PostMapping
    public ResponseEntity<SessionResponse> createManual(@RequestBody @Valid SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createManual(request));
    }

    /**
     * Cancelación de una sesión. Requiere el usuario que ejecuta la acción para auditoría.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<SessionResponse> cancel(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id,
            Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(sessionService.cancel(id, userId));
    }

    /**
     * Obtener una sesión por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> findById(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id) {
        return ResponseEntity.ok(sessionService.findById(id));
    }

    /**
     * Sesiones de una fecha específica.
     */
    @GetMapping("/fecha/{date}")
    public ResponseEntity<List<SessionResponse>> findByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sessionService.findByDate(date));
    }

    /**
     * Listado paginado de sesiones entre fechas.
     */
    @GetMapping
    public ResponseEntity<Page<SessionResponse>> findBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sessionService.findBetween(desde, hasta, pageable));
    }

    /**
     * Cierre manual de una sesión (fija la hora de fin).
     */
    @PutMapping("/{id}/cerrar")
    public ResponseEntity<SessionResponse> close(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id) {
        return ResponseEntity.ok(sessionService.close(id));
    }

    /**
     * Asignación o cambio de profesor de la sesión.
     */
    @PutMapping("/{id}/profesor/{teacherUserId}")
    public ResponseEntity<SessionResponse> assignTeacher(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id,
            @PathVariable @Positive(message = "El ID de profesor debe ser positivo") Integer teacherUserId) {
        return ResponseEntity.ok(sessionService.assignTeacher(id, teacherUserId));
    }

    /**
     * Resumen de una sesión (totales de asistencia).
     */
    @GetMapping("/{id}/resumen")
    public ResponseEntity<SessionSummaryViewResponse> getSummary(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id) {
        return sessionService.getSummary(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Resúmenes de sesiones por fecha.
     */
    @GetMapping("/resumenes/fecha/{date}")
    public ResponseEntity<List<SessionSummaryViewResponse>> getSummariesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sessionService.getSummariesByDate(date));
    }

    /**
     * Resúmenes paginados entre fechas.
     */
    @GetMapping("/resumenes")
    public ResponseEntity<Page<SessionSummaryViewResponse>> getSummariesBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sessionService.getSummariesBetween(desde, hasta, pageable));
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
