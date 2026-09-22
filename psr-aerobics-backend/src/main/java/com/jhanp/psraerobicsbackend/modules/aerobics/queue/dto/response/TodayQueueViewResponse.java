package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Salida de GET /api/cola/admin, desde vista_fila_hoy. Pantalla de administración: sí lleva dinero.
 * <p>
 * attendanceId, amountCharged y markedAt vienen en null mientras la asistencia no se confirme.
 * position la calcula la vista por sesión.
 */
public record TodayQueueViewResponse(
        Integer checkinId,
        Integer sessionId,
        Integer personId,
        String firstName,
        String lastName,
        OffsetDateTime arrivalTime,
        CheckinMethod checkinMethod,
        Long position,
        Integer attendanceId,
        BigDecimal amountCharged,
        OffsetDateTime markedAt,
        Boolean attended,
        BigDecimal balance,
        String note,

        /**
         * El tope de un reembolso para esta persona. Distinto de balance: no descuenta lo ya
         * consumido en asistencias. Es el número contra el que valida
         * {@code POST /api/personas/{id}/reembolso}, y viaja en esta fila porque el reembolso se
         * inicia desde aquí.
         */
        BigDecimal refundable,

        /**
         * Dinero de la persona que sigue en la caja: lo que entrego de mas y cuyo vuelto nunca se le
         * entrego. No es saldo — no compra clases— ni credito: es una deuda del negocio con ella.
         */
        BigDecimal pendingChange,

        /**
         * Como esta la cuenta, ya resuelto por el servidor: {@code DEBE}, {@code VUELTO_PENDIENTE} o
         * {@code AL_DIA}.
         * <p>
         * Deducirlo comparando el saldo con cero da mal: quien pago 10.000, abono dos dias y consumio
         * uno tiene un dia a favor y no se le debe nada. La regla —y su orden de prioridad— vive en
         * fn_estado_cuenta.
         */
        String accountStatus,

        /**
         * Lo recibido en <b>esta</b> llegada, no en la cuenta entera. Es lo que permite saber si ya se
         * cobro: sin este dato el boton "Cobrar" volvia a su estado inicial y cobrar dos veces era
         * cuestion de distraerse un momento.
         */
        BigDecimal receivedAmount,

        /** Lo aplicado a la cuenta en esta llegada. Los reembolsos de esta llegada ya restan aqui. */
        BigDecimal appliedAmount,

        /**
         * En que momento esta la sesion de esta fila: {@code UPCOMING}, {@code IN_PROGRESS},
         * {@code ENDED} o {@code CANCELLED}. Con el reloj del servidor, que es el unico que no
         * depende de como tenga la hora cada dispositivo.
         */
        String sessionStatus,

        /**
         * <b>La cifra del cobro:</b> lo que hay que cobrarle a esta persona ahora mismo, con su deuda
         * anterior y su saldo a favor ya descontados. 0 si no hay nada que cobrar.
         * <p>
         * Deducirlo de {@code balance} y la tarifa desde el cliente es lo que hacia que a quien debia
         * de antes no se le saldara la deuda con lo que entregaba, y que a quien tenia un dia pagado
         * por adelantado se le volviera a cobrar la sesion. La regla vive en fn_monto_a_pagar.
         */
        BigDecimal amountDue,

        /**
         * true cuando la sesion de hoy ya esta cubierta por dias pagados por adelantado. Puede pasar
         * directo sin cobrarle nada.
         */
        Boolean coveredByCredit
) {
}
