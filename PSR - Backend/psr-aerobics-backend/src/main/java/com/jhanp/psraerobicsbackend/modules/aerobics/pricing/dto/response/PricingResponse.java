package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Salida de GET/PUT /api/tarifa. Es la misma tarifa que cobra fn_tarifa_vigente() al confirmar una
 * asistencia.
 * <p>
 * updatedByUserId sale del JWT en el PUT, nunca del body: es lo que queda en historial_cambio.
 */
public record PricingResponse(
        Integer id,
        BigDecimal amount,
        OffsetDateTime updatedAt,
        Integer updatedByUserId,
        String updatedByName
) {
}
