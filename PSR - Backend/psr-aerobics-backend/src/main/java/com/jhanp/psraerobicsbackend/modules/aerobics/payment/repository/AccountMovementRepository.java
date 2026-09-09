package com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.AccountMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AccountMovementRepository extends JpaRepository<AccountMovement, Integer> {

    Page<AccountMovement> findByPersonIdOrderByTimestampDesc(Integer personId, Pageable pageable);

    List<AccountMovement> findByCheckinId(Integer checkinId);

    List<AccountMovement> findByTimestampBetweenOrderByTimestampDesc(OffsetDateTime from, OffsetDateTime to);

    // Evita emitir dos reembolsos contra el mismo abono.
    boolean existsByOriginalMovementId(Integer originalMovementId);
}
