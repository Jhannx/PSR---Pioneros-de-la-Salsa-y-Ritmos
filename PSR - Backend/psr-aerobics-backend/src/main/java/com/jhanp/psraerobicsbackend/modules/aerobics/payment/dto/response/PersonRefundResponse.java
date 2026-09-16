package com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de POST /api/personas/{personId}/reembolso.
 * <p>
 * Lleva el desglose y no solo el total a propósito: quien atiende necesita poder explicar de dónde
 * salió el dinero, y sin esto la pantalla tendría que volver a pedir los movimientos y reconstruir el
 * reparto — que es justo el cálculo que este endpoint existe para quitarle de encima.
 */
public record PersonRefundResponse(

        Integer personId,

        /** Lo devuelto en total. Coincide con lo pedido: si no alcanzaba, la petición falla entera. */
        BigDecimal totalRefunded,

        /** Un renglón por abono consumido, en el orden en que se consumieron (del más antiguo). */
        List<Allocation> allocations
) {

    /**
     * @param originalMovementId el abono contra el que quedó registrada esta parte
     * @param refundId           el movimiento de REEMBOLSO creado
     * @param amount             cuánto salió de ese abono
     * @param remainingAfter     cuánto le queda por devolver después de esto. Un 0 aquí significa que
     *                           ese abono ya está devuelto del todo
     */
    public record Allocation(
            Integer originalMovementId,
            Integer refundId,
            BigDecimal amount,
            BigDecimal remainingAfter
    ) {
    }
}
