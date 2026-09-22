package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST y PUT /api/metodos-pago.
 * <p>
 * active viaja en el body porque un método de pago nunca se borra — hay movimientos que lo
 * referencian — sino que se apaga. En el alta, null equivale a true.
 */
public record PaymentMethodRequest(
        @NotBlank(message = "El nombre del método de pago es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String name,

        Boolean active
) {
}
