package com.jhanp.psraerobicsbackend.modules.aerobics.history.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.dto.response.ChangeHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.enums.AuditableTable;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.mapper.ChangeHistoryViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.repository.ChangeHistoryViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.service.interfaces.IChangeHistoryService;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Solo lectura. El historial lo alimentan los triggers de auditoría y los procedimientos que
 * auditan por su cuenta; la aplicación nunca inserta aquí a mano.
 * <p>
 * Todo sale de vista_historial_cambios, que además resuelve el nombre de quien hizo el cambio.
 * El enum AuditableTable se envía como texto porque la columna de la vista es el tipo de Postgres.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChangeHistoryService implements IChangeHistoryService {

    private final ChangeHistoryViewRepository changeHistoryViewRepository;
    private final ChangeHistoryViewMapper changeHistoryViewMapper;

    @Override
    public Page<ChangeHistoryViewResponse> findByRecord(AuditableTable affectedTable, Integer recordId, Pageable pageable) {
        log.info("Consultando el historial de cambios de {} ID: {}", affectedTable, recordId);
        return changeHistoryViewRepository
                .findByAffectedTableAndRecordIdOrderByDateTimeDesc(affectedTable.getDbValue(), recordId, pageable)
                .map(changeHistoryViewMapper::toResponse);
    }

    @Override
    public Page<ChangeHistoryViewResponse> findByUser(Integer userId, Pageable pageable) {
        log.info("Consultando el historial de cambios hechos por el usuario ID: {}", userId);
        return changeHistoryViewRepository.findByUserIdOrderByDateTimeDesc(userId, pageable)
                .map(changeHistoryViewMapper::toResponse);
    }

    @Override
    public Page<ChangeHistoryViewResponse> findBetween(OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        log.info("Consultando el historial de cambios entre {} y {}", from, to);
        validateDateRange(from, to);
        return changeHistoryViewRepository.findByDateTimeBetweenOrderByDateTimeDesc(from, to, pageable)
                .map(changeHistoryViewMapper::toResponse);
    }

    private void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null || from.isAfter(to)) {
            log.warn("Rango de fechas inválido: fecha inicio ({}) es posterior a fecha fin ({})", from, to);
            throw new InvalidDataException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}
