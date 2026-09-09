package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Entrada de POST /api/movimientos/{id}/reembolso — devolución real de dinero, distinta de corregir
 * un error de digitación ({@code AccountMovementPatchRequest}).
 * <p>
 * DTO dedicado y no reutilización de AccountMovementRequest: comparten ~35 % de los campos. El
 * reembolso no recibe personId ni checkinId porque los hereda del movimiento original, cuyo id va
 * en el path; el usuario que lo registra sale del JWT.
 */
public record RefundRequest(
        // Positivo: el procedimiento lo guarda en negativo y valida que no supere el abono original.
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal amount,

        @NotNull(message = "El ID del método de pago es obligatorio")
        @Positive(message = "El ID del método de pago debe ser positivo")
        Integer paymentMethodId,

        String note
) {
}
