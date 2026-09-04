package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Entrada de POST /api/movimientos (abono), vía sp_registrar_movimiento.
 * <p>
 * No lleva movementType: el procedimiento inserta siempre 'ABONO'. Un reembolso es otro endpoint y
 * otro DTO ({@code RefundRequest}), porque valida el tope contra el movimiento original.
 * <p>
 * Tampoco lleva userId: quién registra el movimiento sale del JWT (patrón G).
 */
public record AccountMovementRequest(
        @NotNull(message = "El ID de persona es obligatorio")
        @Positive(message = "El ID de persona debe ser positivo")
        Integer personId,

        // null = pago por adelantado, sin llegada asociada
        @Positive(message = "El ID de llegada debe ser positivo")
        Integer checkinId,

        @NotNull(message = "El monto recibido es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto recibido no puede ser negativo")
        BigDecimal amountReceived,

        @NotNull(message = "El monto abonado es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto abonado no puede ser negativo")
        BigDecimal amountApplied,

        @NotNull(message = "El ID del método de pago es obligatorio")
        @Positive(message = "El ID del método de pago debe ser positivo")
        Integer paymentMethodId,

        String note
) {
}
