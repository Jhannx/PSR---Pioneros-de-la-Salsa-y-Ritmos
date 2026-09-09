package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Salida de GET /api/movimientos/vueltos-pendientes, desde vista_vueltos_pendientes: abonos donde se
 * recibió más de lo abonado y el vuelto todavía no se entregó. Se saldan con sp_entregar_vuelto.
 * <p>
 * change lo calcula la vista como monto_recibido - monto_abonado.
 */
public record PendingChangeViewResponse(
        Integer movementId,
        Integer personId,
        String fullName,
        BigDecimal amountReceived,
        BigDecimal amountApplied,
        BigDecimal change,
        OffsetDateTime dateTime
) {
}
