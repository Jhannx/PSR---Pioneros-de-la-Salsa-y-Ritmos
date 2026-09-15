package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Entrada de POST /api/personas/{personId}/reembolso — devolver un monto a una persona sin decir
 * contra qué abono va.
 * <p>
 * La diferencia con {@link RefundRequest} no es de campos sino de responsabilidad. Aquel exige que
 * quien llama ya sepa contra qué movimiento aplica el reembolso, y cuando el monto cruza varios
 * abonos eso obligaba al frontend a calcular el reparto: pedir todos los movimientos, restar lo ya
 * devuelto de cada uno y encadenar una llamada por abono. Eso es regla de negocio, y no es sitio para
 * ella — además cada llamada era su propia transacción, así que un fallo a mitad dejaba medio
 * reembolso aplicado.
 * <p>
 * Aquí solo viaja el total. El reparto lo decide el backend y ocurre entero o no ocurre.
 */
public record PersonRefundRequest(

        /** Positivo. Se reparte entre los abonos con saldo, del más antiguo al más reciente. */
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        BigDecimal amount,

        @NotNull(message = "El ID del método de pago es obligatorio")
        @Positive(message = "El ID del método de pago debe ser positivo")
        Integer paymentMethodId,

        String note
) {
}
