package com.jhanp.psraerobicsbackend.modules.aerobics.session.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.request.SessionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.entity.Attendance;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository.AttendanceRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.repository.CheckinRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionAttendeeResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionSummaryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.mapper.SessionMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.mapper.SessionViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionSummaryViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.service.interfaces.ISessionService;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.event.QueueChangedEvent;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.validation.DateRanges;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SessionService implements ISessionService {

    private final SessionRepository sessionRepository;
    private final CheckinRepository checkinRepository;
    private final AttendanceRepository attendanceRepository;
    private final SessionProcedureRepository sessionProcedureRepository;
    private final SessionSummaryViewRepository sessionSummaryViewRepository;
    private final UserRepository userRepository;
    private final SessionMapper sessionMapper;
    private final SessionViewMapper sessionViewMapper;

    /**
     * Para avisar a las pantallas de cola. Cancelar, cerrar o crear una sesión cambia quién aparece
     * en las tres vistas, y hasta ahora nadie se lo decía a quien ya las tenía abiertas.
     */
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SessionResponse createManual(SessionRequest request) {
        Integer sessionId = sessionProcedureRepository.createManualSession(
                request.date(),
                request.startTime(),
                request.endTime(),
                request.teacherUserId(),
                request.scheduleId()
        );

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException("La sesión no se creó correctamente. ID: " + sessionId));

        log.info("Sesión manual creada: ID {}, Fecha {}, Horario ID {}", 
                sessionId, session.getDate(), session.getSchedule() != null ? session.getSchedule().getId() : "N/A");

        // Una sesión nueva de hoy es una fila nueva donde antes no había ninguna.
        eventPublisher.publishEvent(QueueChangedEvent.sessionChanged(sessionId));

        return sessionMapper.toResponse(session);
    }

    @Override
    @Transactional
    public SessionResponse cancel(Integer id, Integer executedByUserId) {
        sessionProcedureRepository.cancel(id, executedByUserId);
        
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sesión con ID: " + id));

        log.info("Sesión cancelada: ID {} por usuario ID {}", id, executedByUserId);

        // Imprescindible, y era el fallo reportado: las tres vistas de cola filtran por NOT cancelada,
        // así que esta sola escritura saca de la fila a todos los check-ins de la sesión a la vez. Sin
        // el aviso, cualquier pantalla abierta que no sea la que canceló sigue mostrándolos hasta que
        // otra acción cualquiera la haga releer.
        eventPublisher.publishEvent(QueueChangedEvent.sessionChanged(id));

        return sessionMapper.toResponse(session);
    }

    @Override
    public SessionResponse findById(Integer id) {
        return sessionRepository.findById(id)
                .map(sessionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sesión con ID: " + id));
    }

    @Override
    public List<SessionResponse> findByDate(LocalDate date) {
        return sessionRepository.findByDateOrderByStartTimeAsc(date).stream()
                .map(sessionMapper::toResponse)
                .toList();
    }

    /**
     * El rango es opcional aquí, a diferencia del resto de listados por fecha: sin filtros devuelve
     * todas las sesiones, que es lo que pide la pantalla al abrirse, antes de que nadie elija nada.
     * <p>
     * Pero o van los dos o no va ninguno. Con uno solo el rango queda abierto por un extremo y no hay
     * una interpretación evidente —¿desde ahí hasta hoy? ¿hasta siempre?—, así que se rechaza en vez
     * de adivinar.
     */
    @Override
    public Page<SessionResponse> findBetween(LocalDate from, LocalDate to, Pageable pageable) {
        if (from == null && to == null) {
            return sessionRepository.findAllByOrderByDateDescStartTimeDesc(pageable)
                    .map(sessionMapper::toResponse);
        }

        DateRanges.require(from, to);
        return sessionRepository.findByDateBetweenOrderByDateDescStartTimeDesc(from, to, pageable)
                .map(sessionMapper::toResponse);
    }

    /**
     * Listado con filtro de estado. {@code cancelled} en null trae las dos.
     * <p>
     * El filtro se aplica en el servidor y no en el cliente por lo mismo que la busqueda de personas:
     * filtrar despues de paginar da resultados incompletos — una pagina de veinte sesiones de las que
     * cinco estan canceladas se convierte en quince, y la siguiente vuelve a empezar donde no toca.
     */
    @Override
    public Page<SessionResponse> findBetween(LocalDate from, LocalDate to, Boolean cancelled,
                                             Pageable pageable) {
        if (cancelled == null) {
            return findBetween(from, to, pageable);
        }

        if (from == null && to == null) {
            return sessionRepository
                    .findByCancelledOrderByDateDescStartTimeDesc(cancelled, pageable)
                    .map(sessionMapper::toResponse);
        }

        DateRanges.require(from, to);

        return sessionRepository
                .findByDateBetweenAndCancelledOrderByDateDescStartTimeDesc(from, to, cancelled, pageable)
                .map(sessionMapper::toResponse);
    }

    /**
     * Las sesiones asignadas a un profesor. Su pantalla no puede salir del listado general: ese es de
     * personal y muestra las de todo el mundo.
     */
    @Override
    public Page<SessionResponse> findByTeacher(Integer teacherUserId, LocalDate from, LocalDate to,
                                               Pageable pageable) {
        if (from == null && to == null) {
            return sessionRepository
                    .findByTeacherIdOrderByDateDescStartTimeDesc(teacherUserId, pageable)
                    .map(sessionMapper::toResponse);
        }

        DateRanges.require(from, to);

        return sessionRepository
                .findByTeacherIdAndDateBetweenOrderByDateDescStartTimeDesc(teacherUserId, from, to, pageable)
                .map(sessionMapper::toResponse);
    }

    /**
     * Quien llego a una sesion y quien de ellos fue confirmado.
     * <p>
     * Se resuelve con un JOIN FETCH sobre la persona: pedirla fila por fila seria una consulta por
     * asistente, y una clase tiene treinta.
     */
    @Override
    public List<SessionAttendeeResponse> getAttendees(Integer sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("No se encontró la sesión con ID: " + sessionId);
        }

        return checkinRepository.findBySessionIdWithPerson(sessionId).stream()
                .map(checkin -> {
                    Attendance attendance = attendanceRepository.findByCheckinId(checkin.getId()).orElse(null);

                    return new SessionAttendeeResponse(
                            checkin.getPerson().getId(),
                            checkin.getPerson().getName() + " " + checkin.getPerson().getLastName(),
                            checkin.getArrivalTime(),
                            attendance != null,
                            attendance != null ? attendance.getMarkedAt() : null,
                            attendance != null ? attendance.getAmountCharged() : null);
                })
                .toList();
    }

    /**
     * ¿Es esta sesion de este profesor? Lo necesita el control de permisos de "finalizar la mia".
     */
    @Override
    public boolean isTeacherOf(Integer sessionId, Integer teacherUserId) {
        return sessionRepository.findById(sessionId)
                .map(Session::getTeacher)
                .map(teacher -> teacher.getId().equals(teacherUserId))
                .orElse(false);
    }

    @Override
    public Optional<SessionResponse> findOpenSession(LocalDate date) {
        return sessionRepository.findByDateAndEndTimeIsNullAndCancelledFalse(date)
                .map(sessionMapper::toResponse);
    }

    /**
     * Cierra la sesion fijando su hora de fin en ahora.
     * <p>
     * <b>No hace nada si la sesion ya habia terminado</b>, y esa guarda importa: cerrar una clase que
     * acabo a las 19:00 cuando son las 21:00 movia su hora de fin dos horas hacia adelante, inventando
     * una duracion que no fue y, peor, volviendola "vigente" otra vez para el check-in. Un boton de
     * Finalizar que llega tarde no deberia poder reabrir nada.
     */
    @Override
    @Transactional
    public SessionResponse close(Integer id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sesión con ID: " + id));

        if (session.getEndTime() != null && session.getEndTime().isBefore(OffsetDateTime.now())) {
            log.info("La sesión {} ya había terminado a las {}: el cierre no la toca",
                    id, session.getEndTime());
            return sessionMapper.toResponse(session);
        }

        session.setEndTime(OffsetDateTime.now());
        Session saved = sessionRepository.save(session);
        
        log.info("Sesión cerrada manualmente: ID {}", id);

        // Cerrarla cambia quién puede seguir llegando y la ventana de la fila de prepagados.
        eventPublisher.publishEvent(QueueChangedEvent.sessionChanged(id));

        return sessionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SessionResponse assignTeacher(Integer id, Integer teacherUserId) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sesión con ID: " + id));
        
        if (teacherUserId != null) {
            session.setTeacher(userRepository.getReferenceById(teacherUserId));
        } else {
            session.setTeacher(null);
        }
        
        Session saved = sessionRepository.save(session);
        log.info("Profesor asignado a la sesión ID {}: Usuario ID {}", id, teacherUserId);
        return sessionMapper.toResponse(saved);
    }

    @Override
    public Optional<SessionSummaryViewResponse> getSummary(Integer id) {
        return sessionSummaryViewRepository.findBySessionId(id)
                .map(sessionViewMapper::toResponse);
    }

    @Override
    public List<SessionSummaryViewResponse> getSummariesByDate(LocalDate date) {
        return sessionSummaryViewRepository.findByDateOrderByStartTimeDesc(date).stream()
                .map(sessionViewMapper::toResponse)
                .toList();
    }

    @Override
    public Page<SessionSummaryViewResponse> getSummariesBetween(LocalDate from, LocalDate to, Pageable pageable) {
        DateRanges.require(from, to);
        return sessionSummaryViewRepository.findByDateBetweenOrderByDateDesc(from, to, pageable)
                .map(sessionViewMapper::toResponse);
    }
}
