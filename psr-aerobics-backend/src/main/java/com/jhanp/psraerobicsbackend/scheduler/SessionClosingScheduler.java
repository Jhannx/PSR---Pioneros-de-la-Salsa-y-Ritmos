package com.jhanp.psraerobicsbackend.scheduler;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.event.QueueChangedEvent;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionProcedureRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cierra las sesiones que el admin olvidó cerrar.
 * <p>
 * Solo afecta a las sesiones manuales creadas sin hora de fin: las que nacen de un horario ya vienen
 * con hora_fin puesta. Sin esto, una sesión suelta queda abierta para siempre y
 * fn_sesion_abierta_ahora() la seguiría dando por vigente, mandando los check-in de días siguientes
 * a la fila de una clase que ya terminó.
 * <p>
 * El cierre queda auditado a nombre del usuario SISTEMA, así que en el historial se distingue de un
 * cierre hecho a mano por un admin.
 */
@Component
public class SessionClosingScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionClosingScheduler.class);

    /**
     * Margen desde el fin previsto de la clase, no desde su inicio. El fin previsto lo saca el
     * procedimiento del horario asociado; si la sesión es suelta, asume una hora de duración.
     * <p>
     * Con este valor, una clase de 6:00 a 7:00 se cierra a las 9:00 — dos horas de margen para que
     * el admin la cierre él mismo.
     */
    private static final int GRACE_HOURS = 2;

    private final SessionProcedureRepository sessionProcedureRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SessionClosingScheduler(SessionProcedureRepository sessionProcedureRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.sessionProcedureRepository = sessionProcedureRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * fixedDelay y no fixedRate: si una ejecución se demora, la siguiente espera en vez de solaparse.
     */
    @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
    public void closeExpiredSessions() {
        try {
            Integer closed = sessionProcedureRepository.closeExpiredSessions(GRACE_HOURS);

            if (closed != null && closed > 0) {
                log.info("Cierre automático de sesiones: {} sesión(es) cerrada(s) tras {} horas sin cerrar",
                        closed, GRACE_HOURS);

                // Cerrar una sesion cambia quien sigue en la fila, asi que las pantallas abiertas
                // tienen que enterarse igual que cuando alguien la cierra a mano. Sin sesion concreta:
                // la pasada puede haber cerrado varias.
                eventPublisher.publishEvent(QueueChangedEvent.sessionChanged(null));
            }
        } catch (Exception e) {
            // Se traga la excepción a propósito: si falla una pasada, el scheduler debe seguir vivo
            // para intentarlo en la siguiente.
            log.error("Falló el cierre automático de sesiones vencidas", e);
        }
    }
}
