package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.interfaces.IScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestión de horarios habituales de clases.
 * <p>
 * Un horario nunca se borra físicamente (hay sesiones ligadas); desactivarlo fija active = false (HTTP 204).
 */
@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
@Validated
public class ScheduleController {

    private final IScheduleService scheduleService;

    /**
     * Consulta horarios con filtros opcionales de estado activo y/o día de la semana (1 = lunes ... 7 = domingo).
     */
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> findAll(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) @Min(value = 1, message = "El día debe ser entre 1 y 7") @Max(value = 7, message = "El día debe ser entre 1 y 7") Integer diaSemana) {

        if (diaSemana != null) {
            return ResponseEntity.ok(scheduleService.findByDayOfWeek(diaSemana));
        }
        if (Boolean.TRUE.equals(activo)) {
            return ResponseEntity.ok(scheduleService.findAllActive());
        }
        return ResponseEntity.ok(scheduleService.findAll());
    }

    /**
     * Consulta un horario por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> findById(
            @PathVariable @Positive(message = "El ID del horario debe ser positivo") Integer id) {

        return ResponseEntity.ok(scheduleService.findById(id));
    }

    /**
     * Registra un nuevo horario semanal.
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @Valid @RequestBody ScheduleRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }

    /**
     * Actualiza un horario existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> update(
            @PathVariable @Positive(message = "El ID del horario debe ser positivo") Integer id,
            @Valid @RequestBody ScheduleRequest request) {

        return ResponseEntity.ok(scheduleService.update(id, request));
    }

    /**
     * Desactiva un horario (borrado lógico).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "El ID del horario debe ser positivo") Integer id) {

        scheduleService.setActive(id, false);
        return ResponseEntity.noContent().build();
    }
}
