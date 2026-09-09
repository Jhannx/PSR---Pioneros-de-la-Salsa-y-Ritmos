package com.jhanp.psraerobicsbackend.modules.person.repository;

import com.jhanp.psraerobicsbackend.modules.person.view.PersonBalanceView;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Solo lectura sobre vista_saldo_persona. Extiende Repository y no JpaRepository a propósito:
 * así no quedan expuestos save() ni delete() sobre una vista.
 */
public interface PersonBalanceViewRepository extends Repository<PersonBalanceView, Integer> {

    Optional<PersonBalanceView> findByPersonId(Integer personId);

    /**
     * Saldos de menor a mayor: los deudores primero. La vista solo incluye personas activas.
     */
    List<PersonBalanceView> findAllByOrderByBalanceAsc();
}
