package com.jhanp.psraerobicsbackend.modules.person.dto.response;

import java.math.BigDecimal;

/**
 * Salida de GET /api/personas/{id}/saldo, desde vista_saldo_persona. La vista solo incluye personas
 * activas.
 * <p>
 * balance negativo significa que la persona debe.
 */
public record PersonBalanceViewResponse(
        Integer personId,
        String firstName,
        String lastName,
        BigDecimal totalPaid,
        BigDecimal totalCharged,
        BigDecimal balance
) {
}
