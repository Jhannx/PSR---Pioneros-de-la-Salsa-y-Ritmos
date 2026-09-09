package com.jhanp.psraerobicsbackend.modules.person.repository;

import com.jhanp.psraerobicsbackend.modules.person.view.PersonProfileView;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Solo lectura sobre vista_perfil_persona: datos, contacto de emergencia, saldo, días de crédito
 * y resumen de asistencias en una sola consulta.
 */
public interface PersonProfileViewRepository extends Repository<PersonProfileView, Integer> {

    Optional<PersonProfileView> findByPersonId(Integer personId);

    Optional<PersonProfileView> findByDocumentNumber(String documentNumber);
}
