package com.jhanp.psraerobicsbackend.modules.aerobics.history.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.view.ChangeHistoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Solo lectura sobre vista_historial_cambios: igual que historial_cambio pero con el nombre de
 * quien hizo el cambio ya resuelto en performedBy.
 */
public interface ChangeHistoryViewRepository extends Repository<ChangeHistoryView, Integer> {

    Page<ChangeHistoryView> findByAffectedTableAndRecordIdOrderByDateTimeDesc(String affectedTable,
                                                                             Integer recordId,
                                                                             Pageable pageable);

    Page<ChangeHistoryView> findByUserIdOrderByDateTimeDesc(Integer userId, Pageable pageable);

    Page<ChangeHistoryView> findByDateTimeBetweenOrderByDateTimeDesc(OffsetDateTime from,
                                                                     OffsetDateTime to,
                                                                     Pageable pageable);
}
