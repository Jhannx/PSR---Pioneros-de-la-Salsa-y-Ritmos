package com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.request.QueueConfigRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.QueueConfigResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.entity.QueueConfig;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.mapper.QueueConfigMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository.QueueConfigRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces.IQueueConfigService;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.persistence.AuditContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueueConfigService implements IQueueConfigService {

    private final QueueConfigRepository queueConfigRepository;
    private final QueueConfigMapper queueConfigMapper;
    private final AuditContextRepository auditContextRepository;
    private final UserRepository userRepository;

    @Override
    public QueueConfigResponse getCurrent() {
        return queueConfigMapper.toResponse(findCurrent());
    }

    @Override
    @Transactional
    public QueueConfigResponse update(QueueConfigRequest request, Integer updatedByUserId) {
        // Patrón C: el trigger audita los tres campos por separado y necesita app.usuario_id aquí.
        auditContextRepository.setCurrentUser(updatedByUserId);

        QueueConfig config = findCurrent();
        config.setPersonsPerTurn(request.peoplePerTurn());
        config.setEntryMarginMinutes(request.entryMarginMinutes());
        config.setPaymentVisibilityMinutes(request.paymentVisibilityMinutes());
        config.setUpdatedBy(userRepository.getReferenceById(updatedByUserId));

        log.info("Configuración de cola actualizada por el usuario ID: {}", updatedByUserId);
        return queueConfigMapper.toResponse(queueConfigRepository.save(config));
    }

    private QueueConfig findCurrent() {
        return queueConfigRepository.findById(QueueConfigRepository.CURRENT_QUEUE_CONFIG_ID)
                .orElseThrow(() -> new ResourceNotFoundException("No hay configuración de cola registrada en el sistema"));
    }
}
