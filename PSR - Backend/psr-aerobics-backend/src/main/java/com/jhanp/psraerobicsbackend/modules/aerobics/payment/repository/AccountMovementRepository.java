package com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.AccountMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountMovementRepository extends JpaRepository<AccountMovement, Integer> {

    Page<AccountMovement> findByPersonIdOrderByTimestampDesc(Integer personId, Pageable pageable);

    List<AccountMovement> findByCheckinId(Integer checkinId);

    List<AccountMovement> findByTimestampBetweenOrderByTimestampDesc(OffsetDateTime from, OffsetDateTime to);

    // Evita emitir dos reembolsos contra el mismo abono.
    boolean existsByOriginalMovementId(Integer originalMovementId);

    /**
     * El reembolso que acaba de emitir sp_registrar_reembolso contra un abono. El procedimiento no
     * devuelve el id, así que hay que ir a buscarlo (patrón H).
     * <p>
     * Existe para no traer la tabla entera a memoria y filtrarla en Java, que es como estaba resuelto:
     * eso crecía con el histórico de movimientos y acababa costando un reembolso más caro cada mes.
     * El desempate por fecha descendente se conserva del filtro anterior; en la práctica solo hay un
     * reembolso por abono (existsByOriginalMovementId lo impide), pero pedir el más reciente es lo
     * correcto si algún día se permite más de uno.
     */
    Optional<AccountMovement> findTopByOriginalMovementIdOrderByTimestampDesc(Integer originalMovementId);
}
