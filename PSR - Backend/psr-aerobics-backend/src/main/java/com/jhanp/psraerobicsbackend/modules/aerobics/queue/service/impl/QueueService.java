package com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PrepaidQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.TodayQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.WaitingQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.mapper.QueueViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.PrepaidQueueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.TodayQueueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.WaitingQueueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces.IQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<TodayQueueViewResponse> getTodayQueue() {
        log.info("Consultando la cola completa del día para el panel de administración");
        return todayQueueViewRepository.findAllByOrderByArrivalTimeAsc().stream()
                .map(queueViewMapper::toResponse)
                .toList();
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

    @Override
    public List<WaitingQueueViewResponse> getWaitingQueue() {
        log.info("Consultando la fila de espera para la pantalla de la sala");
        return waitingQueueViewRepository.findAllByOrderByArrivalTimeAsc().stream()
                .map(queueViewMapper::toResponse)
                .toList();
    }

    @Override
    public List<PrepaidQueueViewResponse> getPrepaidQueue() {
        log.info("Consultando la fila de prepagados para la pantalla de la sala");
        return prepaidQueueViewRepository.findAllByOrderByArrivalTimeAsc().stream()
                .map(queueViewMapper::toResponse)
                .toList();
    }
}
