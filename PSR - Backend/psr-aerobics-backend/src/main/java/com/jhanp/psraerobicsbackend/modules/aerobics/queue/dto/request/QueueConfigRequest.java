package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada de PUT /api/cola/configuracion. updatedByUserId no va en el body: sale del JWT (patrón G),
 * y es lo que el trigger de auditoría registra como autor del cambio.
 */
public record QueueConfigRequest(
        @NotNull(message = "Las personas por turno es obligatorio")
        @Min(value = 1, message = "Las personas por turno debe ser al menos 1")
        Integer peoplePerTurn,

        @NotNull(message = "Los minutos de margen de ingreso es obligatorio")
        @Min(value = 0, message = "Los minutos de margen no pueden ser negativos")
        Integer entryMarginMinutes,

        @NotNull(message = "Los minutos de visibilidad de pagos es obligatorio")
        @Min(value = 0, message = "Los minutos de visibilidad no pueden ser negativos")
        Integer paymentVisibilityMinutes
) {
}
