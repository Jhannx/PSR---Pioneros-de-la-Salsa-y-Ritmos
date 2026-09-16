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
import com.jhanp.psraerobicsbackend.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
// Consultar el horario lo necesita todo el personal; modificarlo es administración.
@PreAuthorize(Authorities.STAFF)
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
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<ScheduleResponse> create(
            @Valid @RequestBody ScheduleRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
    }

    /**
     * Actualiza un horario existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<ScheduleResponse> update(
            @PathVariable @Positive(message = "El ID del horario debe ser positivo") Integer id,
            @Valid @RequestBody ScheduleRequest request) {

        return ResponseEntity.ok(scheduleService.update(id, request));
    }

    /**
     * Reactiva un horario desactivado: vuelve a generar sesiones por recurrencia
     * (fn_obtener_o_crear_sesion_del_momento solo mira los horarios con activo = true).
     * <p>
     * Sin este endpoint, el DELETE de abajo era un viaje de ida: apagaba el horario y no había forma
     * de encenderlo otra vez desde la API. Mismo patrón que /api/usuarios/{id}/activar.
     */
    @PatchMapping("/{id}/activar")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<ScheduleResponse> activate(
            @PathVariable @Positive(message = "El ID del horario debe ser positivo") Integer id) {

        return ResponseEntity.ok(scheduleService.setActive(id, true));
    }

    /**
     * Desactiva un horario. Hace lo mismo que el DELETE de abajo, pero devuelve el horario con su
     * estado nuevo en vez de un 204 mudo, y se llama como lo que realmente hace.
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<ScheduleResponse> deactivate(
            @PathVariable @Positive(message = "El ID del horario debe ser positivo") Integer id) {

        return ResponseEntity.ok(scheduleService.setActive(id, false));
    }

    /**
     * Desactiva un horario (borrado lógico).
     * <p>
     * Se mantiene por compatibilidad con el frontend que ya lo llama; para nuevas pantallas es
     * preferible el PATCH /desactivar, que dice lo que hace y devuelve el estado resultante.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "El ID del horario debe ser positivo") Integer id) {

        scheduleService.setActive(id, false);
        return ResponseEntity.noContent().build();
    }
}
