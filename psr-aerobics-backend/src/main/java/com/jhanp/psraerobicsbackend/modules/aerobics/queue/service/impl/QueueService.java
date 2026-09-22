package com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PrepaidQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.TodayQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.WaitingQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.mapper.QueueViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.entity.QueueConfig;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.PrepaidQueueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.QueueConfigRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.TodayQueueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.WaitingQueueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.QueueSummaryResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces.IQueueService;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.TodayQueueView;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.WaitingQueueView;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository.AttendanceRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.AccountMovementRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.mapper.SessionMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Solo lectura: la cola no se escribe nunca desde aquí. Las filas nacen de sp_registrar_llegada y
 * la posición la calcula la vista por sesión.
 * <p>
 * Las tres vistas ya filtran por el día de hoy y descartan sesiones canceladas, así que ninguno de
 * estos métodos necesita recibir la fecha.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueueService implements IQueueService {

    private final TodayQueueViewRepository todayQueueViewRepository;
    private final WaitingQueueViewRepository waitingQueueViewRepository;
    private final PrepaidQueueViewRepository prepaidQueueViewRepository;
    private final QueueViewMapper queueViewMapper;

    /** Para el resumen: la clase vigente puede no tener todavia a nadie en la fila. */
    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;

    /** Las dos cifras del encabezado: lo cobrado por esta clase y lo abonado hoy. */
    private final AttendanceRepository attendanceRepository;
    private final AccountMovementRepository accountMovementRepository;
    private final QueueConfigRepository queueConfigRepository;

    @Override
    public List<TodayQueueViewResponse> getTodayQueue() {
        log.info("Consultando la cola completa del día para el panel de administración");
        return todayQueueViewRepository.findAllByOrderByArrivalTimeAsc().stream()
                .map(queueViewMapper::toResponse)
                .toList();
    }

    /**
     * La cola de la sesion vigente, que es lo que recepcion tiene delante.
     * <p>
     * Existe porque devolver el dia entero mezclaba en una sola lista a quien esta esperando ahora con
     * quien ya paso por la clase de la manana: la lista no se "reiniciaba" al terminar una sesion y se
     * seguia ofreciendo cobrar sobre filas de una clase que ya acabo.
     * <p>
     * Cual es la vigente lo decide el servidor, por el estado que ya calcula la vista:
     * <ol>
     *   <li>la que esta en curso;</li>
     *   <li>si no hay ninguna, la proxima que empiece;</li>
     *   <li>si tampoco, la ultima que termino — al final del dia lo que se quiere ver es la de hoy,
     *       no una pantalla vacia.</li>
     * </ol>
     * Con dos clases seguidas esto cambia solo de una a otra sin que nadie toque nada.
     */
    @Override
    public List<TodayQueueViewResponse> getCurrentSessionQueue() {
        List<TodayQueueView> rows = todayQueueViewRepository.findAllByOrderByArrivalTimeAsc();

        Integer currentSessionId = currentSessionId(rows);

        if (currentSessionId == null) {
            log.info("No hay ninguna sesión con filas hoy: la cola vigente va vacía");
            return List.of();
        }

        log.info("Consultando la cola de la sesión vigente ({})", currentSessionId);

        return rows.stream()
                .filter(row -> currentSessionId.equals(row.getSessionId()))
                .map(queueViewMapper::toResponse)
                .toList();
    }

    /**
     * Se elige entre las sesiones que tienen filas hoy, no entre todas las del dia: una sesion sin
     * nadie en la cola no tiene nada que mostrar, asi que elegirla solo dejaria la pantalla en blanco
     * teniendo gente esperando en otra.
     * <p>
     * <b>Sin recurso a la ultima del dia, y esto cambio el 21-sep.</b> Antes, si no habia ninguna en
     * curso ni proxima, se devolvia la ultima que hubiera terminado; la idea era no dejar la pantalla
     * vacia al final del dia. En la prueba se vio que eso es peor: la lista seguia mostrando a la gente
     * de la clase cerrada, con sus contadores, y se podia seguir cobrando sobre esas filas. Terminada
     * la ultima sesion, la respuesta correcta es que no hay cola.
     */
    private Integer currentSessionId(List<TodayQueueView> rows) {
        return rows.stream()
                .filter(row -> "IN_PROGRESS".equals(row.getSessionStatus()))
                .map(TodayQueueView::getSessionId)
                .findFirst()
                .or(() -> rows.stream()
                        .filter(row -> "UPCOMING".equals(row.getSessionStatus()))
                        .map(TodayQueueView::getSessionId)
                        .findFirst())
                .orElse(null);
    }

    /**
     * El encabezado de la pantalla de cola: si hay clase en curso, cual es la proxima y a que hora.
     * <p>
     * Sale de la tabla de sesiones y no de las filas de la cola, a diferencia de
     * {@link #getCurrentSessionQueue()}: una clase que todavia no tiene a nadie en la fila sigue siendo
     * la proxima, y es justo lo que hay que anunciar cuando la anterior acaba de cerrar.
     */
    @Override
    public QueueSummaryResponse getSummary(boolean includeMoney) {
        Session current = currentSession();

        if (current == null) {
            return new QueueSummaryResponse(null, null,
                    includeMoney ? collectedToday() : null);
        }

        return new QueueSummaryResponse(
                sessionMapper.toResponse(current),
                includeMoney ? collectedInSession(current.getId()) : null,
                includeMoney ? collectedToday() : null);
    }

    /**
     * La sesion de la que se esta hablando ahora mismo: la que esta en curso, si no la proxima de hoy,
     * si no la ultima de hoy.
     * <p>
     * La tercera rama es la diferencia con {@link #getCurrentSessionQueue()}, que devuelve vacio
     * cuando ya no queda ninguna viva. No es una contradiccion: la cola responde "a quien atiendo", y
     * cuando la ultima clase termino la respuesta correcta es "a nadie"; esto responde "de que clase
     * estamos hablando", y ahi la ultima del dia sigue siendo la respuesta util — es la que se puede
     * consultar, cerrar o mirar.
     * <p>
     * Sale de la tabla y no de las filas: una clase en curso sin nadie en la cola sigue siendo la
     * vigente, y es justo el caso en que el cliente no tenia de donde sacar su id.
     */
    private Session currentSession() {
        OffsetDateTime now = OffsetDateTime.now();

        List<Session> today = sessionRepository.findByDateOrderByStartTimeAsc(LocalDate.now()).stream()
                .filter(session -> !Boolean.TRUE.equals(session.getCancelled()))
                .toList();

        return today.stream()
                .filter(session -> !session.getStartTime().isAfter(now))
                .filter(session -> session.getEndTime() == null || session.getEndTime().isAfter(now))
                .findFirst()
                .or(() -> today.stream()
                        .filter(session -> session.getStartTime().isAfter(now))
                        .findFirst())
                .or(() -> today.isEmpty()
                        ? java.util.Optional.empty()
                        : java.util.Optional.of(today.get(today.size() - 1)))
                .orElse(null);
    }

    private BigDecimal collectedInSession(Integer sessionId) {
        BigDecimal collected = attendanceRepository.sumChargedBySession(sessionId);
        return collected != null ? collected : BigDecimal.ZERO;
    }

    private BigDecimal collectedToday() {
        BigDecimal collected = accountMovementRepository.findCollectedToday();
        return collected != null ? collected : BigDecimal.ZERO;
    }

    @Override
    public List<TodayQueueViewResponse> getTodayQueueBySession(Integer sessionId) {
        log.info("Consultando la cola de la sesión ID: {}", sessionId);
        return todayQueueViewRepository.findBySessionIdOrderByPositionAsc(sessionId).stream()
                .map(queueViewMapper::toResponse)
                .toList();
    }

    @Override
    public List<TodayQueueViewResponse> getPendingBySession(Integer sessionId) {
        log.info("Consultando los pendientes de atender de la sesión ID: {}", sessionId);
        return todayQueueViewRepository.findBySessionIdAndAttendedFalseOrderByPositionAsc(sessionId).stream()
                .map(queueViewMapper::toResponse)
                .toList();
    }

    /**
     * Fila de espera filtrada por la sesion vigente: en curso o la proxima. Si no hay ninguna viva,
     * devuelve vacio.
     * <p>
     * Antes devolvia todas las llegadas del dia, y la pantalla de la TV mezclaba a quien espera ahora
     * con los de una clase ya terminada.
     */
    @Override
    public List<WaitingQueueViewResponse> getWaitingQueue() {
        log.info("Consultando la fila de espera para la pantalla de la sala");

        Session current = currentLiveSession();
        if (current == null) {
            log.info("No hay sesión viva: la fila de espera va vacía");
            return List.of();
        }

        Integer sessionId = current.getId();
        return waitingQueueViewRepository.findBySessionIdOrderByPositionAsc(sessionId).stream()
                .map(queueViewMapper::toResponse)
                .toList();
    }

    /**
     * Prepagados filtrados por la sesion vigente, incluyendo a quienes fueron confirmados
     * automaticamente por credito al hacer check-in.
     * <p>
     * <b>Por que hace falta un segundo origen de datos:</b> {@code vista_fila_prepagada} exige
     * {@code a.id IS NULL} (sin asistencia), pero {@code confirmIfCoveredByCredit} confirma la
     * asistencia en el acto cuando el credito cubre la sesion. En ese instante la persona desaparece
     * de la vista prepagada aunque acabe de llegar y deba verse en "Pasan directo".
     * <p>
     * La solucion: complementar con filas de {@code vista_fila_hoy} que ya estan confirmadas, tienen
     * {@code monto_a_pagar = 0} (no debian nada, es decir la sesion estaba cubierta por credito) y
     * cuya hora de llegada cae dentro de la ventana {@code paymentVisibilityMinutes}.
     */
    @Override
    public List<PrepaidQueueViewResponse> getPrepaidQueue() {
        log.info("Consultando la fila de prepagados para la pantalla de la sala");

        Session current = currentLiveSession();
        if (current == null) {
            log.info("No hay sesión viva: la fila de prepagados va vacía");
            return List.of();
        }

        Integer sessionId = current.getId();

        // 1. Los que aun no fueron confirmados y tienen credito (la vista original).
        List<PrepaidQueueViewResponse> fromView = prepaidQueueViewRepository
                .findAllByOrderByArrivalTimeAsc().stream()
                .map(queueViewMapper::toResponse)
                .toList();

        // 2. Los confirmados automaticamente por credito: ya tienen asistencia, pero la sesion
        //    estaba cubierta (monto_a_pagar = 0) y llegaron dentro de la ventana de visibilidad.
        int visibilityMinutes = queueConfigRepository
                .findById(QueueConfigRepository.CURRENT_QUEUE_CONFIG_ID)
                .map(QueueConfig::getPaymentVisibilityMinutes)
                .orElse(15);

        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(visibilityMinutes);

        List<PrepaidQueueViewResponse> autoConfirmed = todayQueueViewRepository
                .findBySessionIdOrderByPositionAsc(sessionId).stream()
                .filter(row -> Boolean.TRUE.equals(row.getAttended()))
                .filter(row -> row.getAmountDue() != null
                        && row.getAmountDue().compareTo(BigDecimal.ZERO) == 0)
                .filter(row -> row.getArrivalTime() != null
                        && row.getArrivalTime().isAfter(cutoff))
                .map(row -> new PrepaidQueueViewResponse(
                        row.getCheckinId(), row.getFirstName(),
                        row.getLastName(), row.getArrivalTime()))
                .toList();

        // Unir ambas listas, sin duplicar (una fila no puede estar en las dos: la vista excluye
        // a los confirmados).
        List<PrepaidQueueViewResponse> merged = new ArrayList<>(fromView.size() + autoConfirmed.size());
        merged.addAll(fromView);
        merged.addAll(autoConfirmed);
        merged.sort((a, b) -> a.arrivalTime().compareTo(b.arrivalTime()));

        return merged;
    }

    /**
     * La sesion viva ahora mismo: la que esta en curso, si no la proxima de hoy. Sin recurso a la
     * ultima que termino — las pantallas publicas no deben seguir mostrando datos de una clase cerrada.
     * <p>
     * Se resuelve desde la tabla de sesiones (no desde las filas de la cola) porque una sesion puede
     * existir sin que nadie haya llegado todavia.
     */
    private Session currentLiveSession() {
        OffsetDateTime now = OffsetDateTime.now();

        List<Session> today = sessionRepository.findByDateOrderByStartTimeAsc(LocalDate.now()).stream()
                .filter(session -> !Boolean.TRUE.equals(session.getCancelled()))
                .toList();

        return today.stream()
                .filter(session -> !session.getStartTime().isAfter(now))
                .filter(session -> session.getEndTime() == null || session.getEndTime().isAfter(now))
                .findFirst()
                .or(() -> today.stream()
                        .filter(session -> session.getStartTime().isAfter(now))
                        .findFirst())
                .orElse(null);
    }
}
