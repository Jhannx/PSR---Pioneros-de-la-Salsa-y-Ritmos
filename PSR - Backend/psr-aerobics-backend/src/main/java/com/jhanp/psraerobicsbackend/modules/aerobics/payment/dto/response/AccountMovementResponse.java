package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.enums.MovementType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Salida de los endpoints de movimiento (abono, corrección y reembolso).
 * <p>
 * sp_registrar_movimiento y sp_registrar_reembolso no devuelven nada, así que id, dateTime y el
 * nombre del método de pago salen de releer movimiento_cuenta después del CALL (patrón H).
 * <p>
 * paymentMethodName acompaña al id para que la pantalla no tenga que cruzar contra
 * GET /api/metodos-pago solo para mostrar el nombre.
 */
public record AccountMovementResponse(
        Integer id,
        Integer personId,
        Integer checkinId,
        BigDecimal amountReceived,
        BigDecimal amountApplied,
        Integer paymentMethodId,
        String paymentMethodName,
        MovementType movementType,
        Integer originalMovementId,
        Boolean changeDelivered,
        OffsetDateTime dateTime,
        Integer userId,
        String note
) {
}
