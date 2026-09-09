package com.jhanp.psraerobicsbackend.modules.aerobics.session.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.request.SessionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionSummaryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.mapper.SessionMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.mapper.SessionViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionSummaryViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.service.interfaces.ISessionService;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SessionProcedureRepository sessionProcedureRepository;
    private final SessionSummaryViewRepository sessionSummaryViewRepository;
    private final UserRepository userRepository;
    private final SessionMapper sessionMapper;
    private final SessionViewMapper sessionViewMapper;

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

        return sessionMapper.toResponse(session);
    }

    @Override
    @Transactional
    public SessionResponse cancel(Integer id, Integer executedByUserId) {
        sessionProcedureRepository.cancel(id, executedByUserId);
        
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sesión con ID: " + id));

        log.info("Sesión cancelada: ID {} por usuario ID {}", id, executedByUserId);
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

    @Override
    public Page<SessionResponse> findBetween(LocalDate from, LocalDate to, Pageable pageable) {
        return sessionRepository.findByDateBetweenOrderByDateDescStartTimeDesc(from, to, pageable)
                .map(sessionMapper::toResponse);
    }

    @Override
    public Optional<SessionResponse> findOpenSession(LocalDate date) {
        return sessionRepository.findByDateAndEndTimeIsNullAndCancelledFalse(date)
                .map(sessionMapper::toResponse);
    }

    @Override
    @Transactional
    public SessionResponse close(Integer id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la sesión con ID: " + id));
        
        session.setEndTime(OffsetDateTime.now());
        Session saved = sessionRepository.save(session);
        
        log.info("Sesión cerrada manualmente: ID {}", id);
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
        return sessionSummaryViewRepository.findByDateBetweenOrderByDateDesc(from, to, pageable)
                .map(sessionViewMapper::toResponse);
    }
}
