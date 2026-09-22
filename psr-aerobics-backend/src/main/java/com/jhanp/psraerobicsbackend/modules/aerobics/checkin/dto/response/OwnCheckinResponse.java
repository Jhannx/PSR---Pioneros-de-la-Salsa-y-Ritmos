package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Salida de {@code GET /api/checkin/propio}: la llegada de hoy de quien pregunta, o nada.
 * <p>
 * Existe porque un miembro no tenia forma de saber si ya habia marcado su llegada. La unica lectura
 * de la cola es de personal y a el le responde 403, asi que su pantalla solo lo recordaba en memoria:
 * al recargar volvia a ofrecerle "Marcar mi llegada" como si no hubiera hecho nada.
 * <p>
 * Lleva mas que {@link CheckinResponse} —turno, si ya fue atendido y en que estado esta la sesion—
 * porque esta pantalla es la unica que esa persona ve: con el 403 de la cola, esto es todo lo que
 * puede saber de su propia clase.
 */
public record OwnCheckinResponse(
        Integer checkinId,
        Integer sessionId,
        OffsetDateTime arrivalTime,

        /** Su turno dentro de la sesion, contado por hora de llegada. */
        Long queuePosition,

        /** true cuando el mostrador ya confirmo su asistencia. */
        Boolean attended,

        /** Lo que se le cobro al confirmar. null mientras no este confirmada. */
        BigDecimal amountCharged,

        /** {@code UPCOMING}, {@code IN_PROGRESS} o {@code ENDED}, con el reloj del servidor. */
        String sessionStatus,

        /**
         * Como esta la sesion de hoy en cuanto al pago, ya resuelto por el servidor:
         * <ul>
         *   <li>{@code PAGADA} — asistencia confirmada y sin deuda;</li>
         *   <li>{@code PAGADA_POR_ADELANTADO} — cubierta con dias que ya tenia a favor: puede pasar
         *       directo sin pasar por el mostrador;</li>
         *   <li>{@code PENDIENTE} — tiene que pagar algo.</li>
         * </ul>
         * Deducirlo de los dias de credito desde la pantalla obliga a repetir la regla del cobro en el
         * cliente, que es de donde salieron los errores de la ronda anterior.
         */
        String paymentStatus,

        /** Lo que tiene que pagar hoy, con su deuda anterior y su saldo ya descontados. 0 si nada. */
        BigDecimal amountDue
) {
}
