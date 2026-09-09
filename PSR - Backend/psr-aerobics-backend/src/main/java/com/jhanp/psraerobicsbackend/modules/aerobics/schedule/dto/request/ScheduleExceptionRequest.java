package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Entrada de POST /api/horarios/{id}/excepciones — "este horario no aplica esta fecha".
 * <p>
 * scheduleId no va en el body: viaja en el path. Repetirlo abriría la puerta a que el body y la URL
 * apunten a horarios distintos.
 */
public record ScheduleExceptionRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate date,

        String reason
) {
}
