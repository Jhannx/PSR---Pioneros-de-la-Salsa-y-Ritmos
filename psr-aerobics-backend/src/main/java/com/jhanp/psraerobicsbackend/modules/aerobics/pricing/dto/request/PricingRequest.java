package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PricingRequest(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El monto debe ser mayor a 0")
        BigDecimal amount
) {
}
