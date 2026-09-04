package com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entrada de POST /api/sesiones — creación manual por parte del admin, vía sp_crear_sesion_manual.
 * <p>
 * Es el único alta de sesión que recibe datos del cliente: la sesión del flujo normal de check-in la
 * crea la base de datos sola desde sp_registrar_llegada.
 */
public record SessionRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate date,

        @NotNull(message = "La hora de inicio es obligatoria")
        OffsetDateTime startTime,

        // null = la sesión sigue en curso. El procedimiento sí recibe este parámetro.
        OffsetDateTime endTime,

        // null = sin asignar; si viene, el procedimiento falla si ese usuario no tiene rol PROFESOR.
        @Positive(message = "El ID del profesor debe ser positivo")
        Integer teacherUserId,

        // null = sesión suelta (taller puntual), sin horario detrás.
        @Positive(message = "El ID del horario debe ser positivo")
        Integer scheduleId
) {
}
