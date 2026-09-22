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
        BigDecimal balance,

        /**
         * El tope de un reembolso, y <b>no es lo mismo que balance</b>: no descuenta lo que la persona
         * ya consumió en asistencias, solo lo que ya se le devolvió.
         * <p>
         * Quien abonó 50.000 y asistió tres días tiene poco balance y bastante refundable. Usar
         * balance como tope de un formulario de devolución produce un 400 al confirmar, porque el
         * servidor valida contra este otro número.
         */
        BigDecimal refundable,

        /**
         * Dinero que la persona entrego de mas y cuyo vuelto nunca se le entrego. No es saldo: no
         * compra clases, es una deuda del negocio con ella.
         */
        BigDecimal pendingChange,

        /**
         * Estado de la cuenta ya resuelto: {@code DEBE}, {@code VUELTO_PENDIENTE} o {@code AL_DIA}.
         * <p>
         * Deducirlo del signo de {@code balance} se equivoca con quien tiene dias pagados por
         * adelantado: tener credito no es un pendiente. La regla vive en fn_estado_cuenta.
         */
        String accountStatus
) {
}
