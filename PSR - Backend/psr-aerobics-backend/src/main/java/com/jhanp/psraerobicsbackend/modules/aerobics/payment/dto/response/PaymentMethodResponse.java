package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response;

public record PaymentMethodResponse(
        Integer id,
        String name,
        Boolean active
) {
}
