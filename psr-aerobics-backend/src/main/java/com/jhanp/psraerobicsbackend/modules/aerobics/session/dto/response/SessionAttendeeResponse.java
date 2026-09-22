package com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Una persona en la lista de asistentes de una sesion: llego, y puede que ademas se le confirmara.
 * <p>
 * Existe porque {@code /resumen} solo da totales — cuantos llegaron y cuantos se confirmaron—, y el
 * profesor necesita nombres: saber quien esta en su clase es la razon de que se le asigne una.
 * <p>
 * Sin dinero mas alla de lo que se le cobro a esa persona por esa clase: el saldo, los vueltos y los
 * reembolsos son del mostrador, no de quien dicta.
 */
public record SessionAttendeeResponse(
        Integer personId,
        String fullName,
        OffsetDateTime arrivalTime,

        /** false si llego pero nadie confirmo su asistencia. */
        boolean attended,

        /** Cuando se confirmo. null si no se confirmo. */
        OffsetDateTime markedAt,

        /** Lo que se le cobro al confirmar. null si no se confirmo. */
        BigDecimal amountCharged
) {
}
