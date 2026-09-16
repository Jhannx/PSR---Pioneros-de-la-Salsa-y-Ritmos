package com.jhanp.psraerobicsbackend.modules.aerobics.queue.event;

/**
 * Aviso de que la cola del día cambió. Lo publican los servicios que escriben —llegada, asistencia,
 * abono— y lo consume {@link QueueEventBroadcaster}.
 * <p>
 * Deliberadamente NO lleva las filas de la cola. Por dos razones: la pantalla de la sala no debe
 * recibir montos ni saldo (RNF-03) y meter el payload completo obligaría a armar una carga distinta
 * por topic, con el riesgo de mandar la equivocada; y las tres vistas calculan posición y
 * visibilidad por su cuenta, así que un payload congelado en el momento del evento podría llegar ya
 * desactualizado. El cliente recibe la señal y relee el endpoint REST que ya tiene permitido.
 */
public record QueueChangedEvent(

        /** Sesión afectada. Va null cuando el cambio no cuelga de ninguna: un pago por adelantado. */
        Integer sessionId,

        ChangeType type
) {

    public enum ChangeType {
        /** Alguien acaba de llegar y entró a la fila. */
        CHECKIN,
        /** Se confirmó la asistencia: esa persona sale de la fila de espera. */
        ATTENDANCE_CONFIRMED,
        /** Se anuló una asistencia y la persona vuelve a la fila. */
        ATTENDANCE_CANCELLED,
        /** Entró un abono: cambia el saldo y puede aparecer en la fila de prepagados. */
        PAYMENT
    }

    public static QueueChangedEvent checkin(Integer sessionId) {
        return new QueueChangedEvent(sessionId, ChangeType.CHECKIN);
    }

    public static QueueChangedEvent attendanceConfirmed(Integer sessionId) {
        return new QueueChangedEvent(sessionId, ChangeType.ATTENDANCE_CONFIRMED);
    }

    public static QueueChangedEvent attendanceCancelled(Integer sessionId) {
        return new QueueChangedEvent(sessionId, ChangeType.ATTENDANCE_CANCELLED);
    }

    public static QueueChangedEvent payment(Integer sessionId) {
        return new QueueChangedEvent(sessionId, ChangeType.PAYMENT);
    }
}
