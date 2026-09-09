package com.jhanp.psraerobicsbackend.modules.person.repository;

import com.jhanp.psraerobicsbackend.modules.person.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Integer> {

    Optional<EmergencyContact> findByPersonId(Integer personId);

    boolean existsByPersonId(Integer personId);
}
