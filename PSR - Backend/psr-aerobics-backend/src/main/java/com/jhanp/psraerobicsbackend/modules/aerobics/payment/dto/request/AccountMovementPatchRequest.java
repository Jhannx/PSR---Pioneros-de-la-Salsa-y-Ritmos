package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Entrada de PATCH /api/movimientos/{id} — corrige un error de digitación sobre un movimiento ya
 * registrado. Todos los campos son opcionales: solo se aplican los que vengan.
 * <p>
 * No es un reembolso. Devolver dinero de verdad es {@code RefundRequest}, que crea un movimiento
 * nuevo en vez de reescribir el original.
 * <p>
 * El UPDATE queda auditado por trigger, así que la implementación debe fijar app.usuario_id con el
 * usuario del JWT en la misma transacción (patrón C).
 */
public record AccountMovementPatchRequest(
        @DecimalMin(value = "0.00", message = "El monto recibido no puede ser negativo")
        BigDecimal amountReceived,

        @DecimalMin(value = "0.00", message = "El monto abonado no puede ser negativo")
        BigDecimal amountApplied,

        @Positive(message = "El ID del método de pago debe ser positivo")
        Integer paymentMethodId,

        String note
) {
}
