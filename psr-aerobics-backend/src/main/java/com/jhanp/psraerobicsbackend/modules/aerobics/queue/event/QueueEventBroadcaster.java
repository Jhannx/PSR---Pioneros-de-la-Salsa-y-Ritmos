package com.jhanp.psraerobicsbackend.modules.aerobics.queue.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Único punto donde la cola habla WebSocket. Los servicios que escriben publican un
 * {@link QueueChangedEvent} y se olvidan: no conocen SimpMessagingTemplate ni los nombres de los
 * topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueEventBroadcaster {

    /** Panel del admin: ve montos, saldo y nota. */
    private static final String TOPIC_ADMIN = "/topic/cola/admin";

    /** Pantalla de la sala: quién está en fila, sin nada de dinero. */
    private static final String TOPIC_WAITING = "/topic/cola/espera";

    /** Pantalla de la sala: quiénes ya pagaron. */
    private static final String TOPIC_PREPAID = "/topic/cola/prepagada";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * AFTER_COMMIT y no un @EventListener normal: si se avisara dentro de la transacción, un rollback
     * posterior dejaría a las pantallas mostrando una llegada que nunca existió. Como efecto
     * secundario, un fallo al difundir tampoco puede tumbar la escritura, que ya está confirmada.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueueChanged(QueueChangedEvent event) {
        try {
            for (String topic : topicsFor(event.type())) {
                messagingTemplate.convertAndSend(topic, event);
            }
            log.debug("Cambio de cola difundido: {} (sesión {})", event.type(), event.sessionId());

        } catch (Exception e) {
            // La difusión es best-effort: si el broker falla, el dato ya está guardado y el cliente
            // lo verá en su próxima consulta. No vale la pena propagar.
            log.warn("No se pudo difundir el cambio de cola {}: {}", event.type(), e.getMessage());
        }
    }

    /**
     * No todo cambio interesa a las tres pantallas. Un abono no mueve la fila de espera, y confirmar
     * asistencia no cambia quién aparece como prepagado.
     */
    private String[] topicsFor(QueueChangedEvent.ChangeType type) {
        return switch (type) {
            case CHECKIN -> new String[]{TOPIC_ADMIN, TOPIC_WAITING, TOPIC_PREPAID};
            case ATTENDANCE_CONFIRMED, ATTENDANCE_CANCELLED -> new String[]{TOPIC_ADMIN, TOPIC_WAITING};
            case PAYMENT -> new String[]{TOPIC_ADMIN, TOPIC_PREPAID};
            // A las tres: cancelar una sesión vacía las tres vistas de golpe, y ninguna puede quedarse
            // mostrando una fila que ya no existe.
            case SESSION_CHANGED -> new String[]{TOPIC_ADMIN, TOPIC_WAITING, TOPIC_PREPAID};
        };
    }
}
