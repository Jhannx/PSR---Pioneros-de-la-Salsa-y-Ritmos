package com.jhanp.psraerobicsbackend.scheduler;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.event.ScheduleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Genera las sesiones de un horario en cuanto ese horario cambia, sin esperar a la pasada horaria.
 * <p>
 * AFTER_COMMIT y REQUIRES_NEW: el horario ya está confirmado cuando esto corre, así que la consulta
 * lo encuentra, y la transacción es otra — si la generación falla, el horario sigue guardado. Esa
 * separación es el punto de todo esto; con una llamada directa dentro de la transacción del horario,
 * un fallo al insertar una sesión se llevaría por delante el horario entero.
 * <p>
 * El error se registra y no se propaga por lo mismo que en la pasada automática: el barrido horario
 * volverá a intentarlo, y una sesión que no se pudo crear no puede tumbar la respuesta de un alta que
 * ya está confirmada en la base.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduleChangeListener {

    private final SessionCreationScheduler sessionCreationScheduler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onScheduleChanged(ScheduleChangedEvent event) {
        try {
            int created = sessionCreationScheduler.createForSchedule(event.scheduleId());

            if (created > 0) {
                log.info("Horario {}: {} sesión(es) generada(s) tras el cambio",
                        event.scheduleId(), created);
            }
        } catch (Exception e) {
            log.error("No se pudieron generar las sesiones del horario {} tras el cambio; "
                    + "quedan para el barrido automático", event.scheduleId(), e);
        }
    }
}
