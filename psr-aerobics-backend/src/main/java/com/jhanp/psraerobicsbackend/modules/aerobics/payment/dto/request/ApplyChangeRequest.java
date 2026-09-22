package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Entrada de {@code POST /api/movimientos/{id}/abonar-vuelto}: convertir en abono el vuelto que quedo
 * pendiente de un cobro.
 * <p>
 * No lleva metodo de pago, y es lo que distingue esta operacion de un abono normal: <b>aqui no entra
 * dinero</b>. El dinero ya se recibio y ya quedo registrado con su metodo en el movimiento original;
 * lo unico que cambia es a que se destina. Pedir un metodo aqui invitaria a contarlo dos veces.
 */
public record ApplyChangeRequest(

        /**
         * Cuanto del vuelto pendiente se convierte en abono. Puede ser una parte: el resto sigue
         * pendiente de entregar.
         */
        @NotNull(message = "El monto a abonar es obligatorio")
        @DecimalMin(value = "1", message = "El monto a abonar debe ser mayor que cero")
        @Digits(integer = 10, fraction = 0,
                message = "El monto debe ser un numero entero de pesos, sin centavos")
        BigDecimal amount,

        String note
) {
}
