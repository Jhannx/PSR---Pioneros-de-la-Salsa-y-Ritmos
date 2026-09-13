package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinByDocumentRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity.Checkin;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.mapper.CheckinMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.repository.CheckinProcedureRepository;
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

        // El broadcaster escucha AFTER_COMMIT, así que esto no llega a las pantallas hasta que la
        // transacción confirme.
        eventPublisher.publishEvent(QueueChangedEvent.checkin(row.getSessionId()));

        return checkinMapper.toResponse(row);
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
