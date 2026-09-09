package com.jhanp.psraerobicsbackend.modules.aerobics.history.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.entity.ChangeHistory;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.enums.AuditableTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Solo lectura en la práctica: las filas las escriben los triggers de auditoría y los procedimientos
 * que auditan por su cuenta. La aplicación no debe insertar aquí a mano.
 */
@Repository
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Integer> {

    List<ChangeHistory> findByAffectedTableAndRecordIdOrderByTimestampDesc(AuditableTable affectedTable,
                                                                          Integer recordId);

    Page<ChangeHistory> findByUserIdOrderByTimestampDesc(Integer userId, Pageable pageable);

    Page<ChangeHistory> findByTimestampBetweenOrderByTimestampDesc(OffsetDateTime from,
                                                                   OffsetDateTime to,
                                                                   Pageable pageable);
}
