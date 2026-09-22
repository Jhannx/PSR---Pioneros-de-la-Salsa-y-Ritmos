package com.jhanp.psraerobicsbackend.scheduler;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.event.QueueChangedEvent;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Avisa a las pantallas cuando una sesion empieza o termina <b>por el reloj</b>.
 * <p>
 * <b>El hueco que tapa.</b> Todos los demas avisos de cola nacen de que alguien escribio algo: una
 * llegada, un pago, una cancelacion. Pero el estado de una sesion cambia solo con el paso del tiempo
 * — a las 11:40 empieza y a las 11:50 termina sin que nadie toque nada— y ahi no habia ningun evento
 * que publicar. El resultado en la prueba del 21-sep fue el esperable: un admin siguio viendo
 * "Finalizar sesion" mucho despues de que la clase acabara, y quien abrio la pantalla antes de que
 * empezara la siguiente nunca vio aparecer el boton.
 * <p>
 * <b>Cada minuto, mirando el minuto que acaba de pasar.</b> La ventana es algo mas ancha que el
 * intervalo a proposito: si una pasada se retrasa unos segundos, un margen justo dejaria transiciones
 * sin avisar, y perder el aviso es peor que repetirlo. Un aviso de mas no cuesta nada — el cliente
 * solo relee por REST— mientras que uno perdido deja la pantalla mintiendo hasta que alguien la toca.
 * <p>
 * fixedDelay y no fixedRate, igual que los otros dos schedulers: si una pasada se demora, la
 * siguiente espera en vez de solaparse.
 *
 * @see SessionCreationScheduler crea las sesiones por adelantado
 * @see SessionClosingScheduler cierra las que se quedaron abiertas
 */
@Component
public class SessionStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionStatusScheduler.class);

    /**
     * Cuanto hacia atras se mira en cada pasada. Mayor que el intervalo para cubrir un retraso de la
     * ejecucion; el solape solo produce un aviso repetido, que es inofensivo.
     */
    private static final int LOOKBACK_SECONDS = 75;

    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * La transaccion se abre a mano en vez de con {@code @Transactional} sobre el metodo, y el motivo
     * es el patron "scheduler que se traga sus errores".
     * <p>
     * Con la anotacion, el try/catch queda <b>dentro</b> de la transaccion: al capturar el fallo, la
     * transaccion ya esta marcada como rollback-only, y el commit que Spring intenta al salir del
     * metodo revienta con UnexpectedRollbackException — fuera del catch, asi que acaba en el handler
     * de tareas programadas como un error sin explicacion. Se ven dos errores en el log por un solo
     * fallo, y el segundo no dice nada del primero.
     * <p>
     * Con el template, la transaccion empieza y termina dentro del try: el catch envuelve tambien al
     * commit, y un fallo se registra una vez y con su causa.
     */
    private final TransactionTemplate transactionTemplate;

    public SessionStatusScheduler(SessionRepository sessionRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  TransactionTemplate transactionTemplate) {
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Hace falta una transaccion aunque aqui no se escriba nada: el broadcaster escucha en
     * AFTER_COMMIT, y un evento publicado fuera de una transaccion se descarta en silencio — sin
     * error, sin log, sin llegar a ninguna pantalla.
     */
    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
    public void announceSessionTransitions() {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                OffsetDateTime now = OffsetDateTime.now();
                OffsetDateTime from = now.minusSeconds(LOOKBACK_SECONDS);

                // El dia va como parametro y no como CURRENT_DATE dentro de la consulta: asi la
                // fecha sale del mismo reloj y la misma zona que los dos instantes del rango.
                for (Session session : sessionRepository
                        .findTransitionedBetween(LocalDate.now(), from, now)) {
                    log.info("Sesión {} cambió de estado por el reloj: ahora {}",
                            session.getId(), session.getStatus());

                    eventPublisher.publishEvent(QueueChangedEvent.sessionChanged(session.getId()));
                }
            });
        } catch (Exception e) {
            // Se traga la excepcion como los otros schedulers: si falla una pasada, el scheduler tiene
            // que seguir vivo para la siguiente.
            log.error("Falló el aviso de cambio de estado de sesiones", e);
        }
    }
}
