package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinByDocumentRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.OwnCheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity.Checkin;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.mapper.CheckinMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.repository.CheckinProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository.AttendanceProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.repository.CheckinRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.interfaces.ICheckinService;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.event.QueueChangedEvent;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.TodayQueueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.TodayQueueView;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CheckinService implements ICheckinService {

    private final CheckinRepository checkinRepository;
    private final CheckinProcedureRepository checkinProcedureRepository;
    private final TodayQueueViewRepository todayQueueViewRepository;
    private final PersonRepository personRepository;
    private final CheckinMapper checkinMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** Para confirmar en el acto la asistencia de quien ya tiene la sesion pagada. */
    private final AttendanceProcedureRepository attendanceProcedureRepository;

    /**
     * La cuenta SISTEMA (id fijo en V1). A ella se atribuyen las confirmaciones automaticas:
     * asistencia.marcado_por es NOT NULL y no hubo ninguna persona detras de esta.
     */
    private static final Integer SYSTEM_USER_ID = 999999998;

    @Override
    @Transactional
    public CheckinResponse register(CheckinRequest request, CheckinMethod checkinMethod) {
        Integer checkinId = checkinProcedureRepository.registerArrival(request.personId(), checkinMethod);

        // Patrón H: El procedimiento devuelve el ID, pero la respuesta completa (con posición) 
        // se lee de la vista_fila_hoy.
        TodayQueueView row = todayQueueViewRepository.findById(checkinId)
                .orElseThrow(() -> new IllegalStateException(
                        "La llegada no aparece en la cola del día después de registrarla. ID: " + checkinId));

        log.info("Check-in registrado: ID {}, Persona {}, Sesión {}, Posición {}",
                checkinId, row.getPersonId(), row.getSessionId(), row.getPosition());

        row = confirmIfCoveredByCredit(row);

        // El broadcaster escucha AFTER_COMMIT, así que esto no llega a las pantallas hasta que la
        // transacción confirme.
        eventPublisher.publishEvent(QueueChangedEvent.checkin(row.getSessionId()));

        return checkinMapper.toResponse(row);
    }

    /**
     * Si la sesion de hoy ya esta pagada con dias adelantados, se confirma la asistencia en el acto y
     * se descuenta la sesion del credito.
     * <p>
     * <b>Por que:</b> quien tiene cinco sesiones pagadas llega y no hay nada que cobrarle, pero su
     * fila quedaba en "sin atender" esperando que alguien del mostrador pulsara un boton que no decide
     * nada. Eso no es una confirmacion, es un tramite — y mientras no ocurriera, el credito no bajaba
     * y la persona figuraba como pendiente.
     * <p>
     * <b>Solo cuando no debe absolutamente nada.</b> {@code cubierto_por_saldo} es cierto unicamente
     * si la asistencia aun no esta confirmada Y lo que hay que cobrarle es cero. Con credito 0, con un
     * abono parcial o con deuda, la fila se queda para el mostrador: ahi si hay una decision que tomar
     * y dinero que recibir.
     * <p>
     * <b>Idempotente por construccion, no por una guarda.</b> La vista pone la columna en false en
     * cuanto existe la asistencia, asi que marcar dos veces la llegada a la misma sesion no puede
     * descontar dos veces. Y como cada llegada es su propia fila, dos clases el mismo dia descuentan
     * una cada una. Una sesion cancelada no aparece en la vista, asi que tampoco descuenta.
     * <p>
     * Queda a nombre del usuario SISTEMA: no lo confirmo nadie del mostrador, y atribuirselo a una
     * persona real seria falso. Deshacerla desde recepcion devuelve la sesion al credito, igual que
     * con cualquier otra.
     */
    private TodayQueueView confirmIfCoveredByCredit(TodayQueueView row) {
        if (!Boolean.TRUE.equals(row.getCoveredByCredit())) {
            return row;
        }

        attendanceProcedureRepository.confirm(row.getCheckinId(), SYSTEM_USER_ID);

        log.info("Asistencia confirmada automaticamente para la llegada {}: la sesion estaba cubierta "
                + "por saldo a favor", row.getCheckinId());

        // Patron H: la fila cambia al confirmar — atendido, monto cobrado y saldo— y hay que releerla
        // para no responder con el estado de hace un instante.
        return todayQueueViewRepository.findById(row.getCheckinId()).orElse(row);
    }

    @Override
    @Transactional
    public CheckinResponse registerByDocument(CheckinByDocumentRequest request) {
        Integer personId = personRepository.findByDocumentNumber(request.documentNumber())
                .map(p -> p.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ninguna persona con el documento " + request.documentNumber()));

        CheckinRequest checkinRequest = new CheckinRequest(personId);

        return register(checkinRequest, CheckinMethod.DOCUMENT);
    }

    @Override
    @Transactional
    public CheckinResponse registerManualByDocument(CheckinByDocumentRequest request) {
        Integer personId = personRepository.findByDocumentNumber(request.documentNumber())
                .map(p -> p.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ninguna persona con el documento " + request.documentNumber()));

        return register(new CheckinRequest(personId), CheckinMethod.MANUAL);
    }

    @Override
    public java.util.Optional<OwnCheckinResponse> findTodayOwn(Integer personId) {
        return todayQueueViewRepository.findFirstByPersonIdOrderByArrivalTimeDesc(personId)
                .map(this::toOwnResponse);
    }

    /**
     * <b>Idempotente de verdad, y no solo por el procedimiento.</b> sp_registrar_llegada ya evita la
     * fila duplicada dentro de una misma sesion, pero eso no basta aqui: hace falta saber si la
     * llegada se acaba de crear o ya estaba, para responder 201 o 200.
     * <p>
     * Se mira la llegada de hoy y se reutiliza <b>solo si su sesion no ha terminado</b>. La diferencia
     * importa el dia que hay dos clases: devolver siempre la de hoy dejaria a quien viene por la
     * tarde sin poder marcar su llegada a la segunda, porque ya tenia una de la manana.
     */
    @Override
    @Transactional
    public OwnCheckinResult registerOwn(Integer personId) {
        java.util.Optional<TodayQueueView> existing =
                todayQueueViewRepository.findFirstByPersonIdOrderByArrivalTimeDesc(personId);

        if (existing.isPresent() && !"ENDED".equals(existing.get().getSessionStatus())) {
            log.info("La persona {} ya tenía llegada hoy (sesión {}): se devuelve la existente",
                    personId, existing.get().getSessionId());

            // Se avisa igual aunque no haya fila nueva, y esto era un fallo real: por esta rama se
            // salia sin publicar nada, asi que la pantalla de recepcion no se enteraba de la llegada
            // y no se actualizaba hasta que alguien agregaba a otra persona a mano. Reavisar de mas no
            // cuesta nada — el cliente solo relee— y no avisar deja la pantalla mintiendo.
            eventPublisher.publishEvent(QueueChangedEvent.checkin(existing.get().getSessionId()));

            return new OwnCheckinResult(toOwnResponse(existing.get()), true);
        }

        CheckinResponse created = register(new CheckinRequest(personId), CheckinMethod.SELF);

        TodayQueueView row = todayQueueViewRepository.findById(created.checkinId())
                .orElseThrow(() -> new IllegalStateException(
                        "La llegada no aparece en la cola del día después de registrarla. ID: "
                                + created.checkinId()));

        return new OwnCheckinResult(toOwnResponse(row), false);
    }

    private OwnCheckinResponse toOwnResponse(TodayQueueView row) {
        return new OwnCheckinResponse(
                row.getCheckinId(), row.getSessionId(), row.getArrivalTime(), row.getPosition(),
                row.getAttended(), row.getAmountCharged(), row.getSessionStatus(),
                paymentStatus(row), row.getAmountDue());
    }

    /**
     * El estado de pago de la sesion de hoy, para que la pantalla del miembro pueda decirle si puede
     * pasar directo.
     * <p>
     * Se deriva de lo que la vista ya calculo — no se vuelve a razonar sobre saldos y tarifas aqui:
     * esa regla vive en fn_monto_a_pagar y tener una segunda version en Java es como empezaron los
     * problemas de las rondas anteriores.
     */
    private String paymentStatus(TodayQueueView row) {
        if (Boolean.TRUE.equals(row.getCoveredByCredit())) {
            return "PAGADA_POR_ADELANTADO";
        }

        boolean nothingDue = row.getAmountDue() == null || row.getAmountDue().signum() == 0;

        if (Boolean.TRUE.equals(row.getAttended()) && nothingDue) {
            return "PAGADA";
        }

        return "PENDIENTE";
    }

    @Override
    public CheckinResponse findById(Integer id) {
        return checkinRepository.findById(id)
                .map(checkinMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el check-in con ID: " + id));
    }

    @Override
    public List<CheckinResponse> findBySession(Integer sessionId) {
        return checkinRepository.findBySessionIdOrderByArrivalTimeAsc(sessionId).stream()
                .map(checkinMapper::toResponse)
                .toList();
    }

    @Override
    public List<CheckinResponse> findByDate(LocalDate date) {
        return checkinRepository.findBySessionDateOrderByArrivalTimeAsc(date).stream()
                .map(checkinMapper::toResponse)
                .toList();
    }

    @Override
    public Page<CheckinResponse> findByPerson(Integer personId, Pageable pageable) {
        return checkinRepository.findByPersonIdOrderByArrivalTimeDesc(personId, pageable)
                .map(checkinMapper::toResponse);
    }

    @Override
    public boolean hasCheckedIn(Integer personId, Integer sessionId) {
        return checkinRepository.existsByPersonIdAndSessionId(personId, sessionId);
    }
}
