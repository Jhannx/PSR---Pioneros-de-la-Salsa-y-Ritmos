package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull(message = "El día de la semana es obligatorio")
        @Min(value = 1, message = "El día de la semana debe estar entre 1 (lunes) y 7 (domingo)")
        @Max(value = 7, message = "El día de la semana debe estar entre 1 (lunes) y 7 (domingo)")
        Integer dayOfWeek,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime startTime,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime endTime
) {
}
