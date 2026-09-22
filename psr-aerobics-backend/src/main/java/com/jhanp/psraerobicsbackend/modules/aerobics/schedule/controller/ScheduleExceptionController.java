package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleExceptionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleExceptionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.interfaces.IScheduleExceptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import com.jhanp.psraerobicsbackend.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Excepciones de horario (días festivos o fechas en que un horario específico no opera).
 */
@RestController
@RequestMapping("/api/horarios")
// Igual que el horario: se consulta en la sala, se modifica desde administración.
@PreAuthorize(Authorities.STAFF)
@RequiredArgsConstructor
@Validated
public class ScheduleExceptionController {

    private final IScheduleExceptionService scheduleExceptionService;

    /**
     * Registra una excepción de fecha para un horario específico.
     */
    @PostMapping("/{horarioId}/excepciones")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<ScheduleExceptionResponse> create(
            @PathVariable("horarioId") @Positive(message = "El ID del horario debe ser positivo") Integer scheduleId,
            @Valid @RequestBody ScheduleExceptionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleExceptionService.create(scheduleId, request));
    }

    /**
     * Lista todas las excepciones registradas para un horario específico.
     */
    @GetMapping("/{horarioId}/excepciones")
    public ResponseEntity<List<ScheduleExceptionResponse>> findBySchedule(
            @PathVariable("horarioId") @Positive(message = "El ID del horario debe ser positivo") Integer scheduleId) {

        return ResponseEntity.ok(scheduleExceptionService.findBySchedule(scheduleId));
    }

    /**
     * Lista excepciones dentro de un rango de fechas.
     */
    @GetMapping("/excepciones")
    public ResponseEntity<List<ScheduleExceptionResponse>> findBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(scheduleExceptionService.findBetween(desde, hasta));
    }

    /**
     * Consulta una excepción de horario por su ID.
     */
    @GetMapping("/excepciones/{id}")
    public ResponseEntity<ScheduleExceptionResponse> findById(
            @PathVariable @Positive(message = "El ID de la excepción debe ser positivo") Integer id) {

        return ResponseEntity.ok(scheduleExceptionService.findById(id));
    }

    /**
     * Elimina una excepción de horario.
     */
    @DeleteMapping("/excepciones/{id}")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "El ID de la excepción debe ser positivo") Integer id) {

        scheduleExceptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
