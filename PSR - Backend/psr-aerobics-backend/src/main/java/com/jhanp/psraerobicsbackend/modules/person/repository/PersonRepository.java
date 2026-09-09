package com.jhanp.psraerobicsbackend.modules.person.repository;

import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Integer> {

    Optional<Person> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);

    @Query("""
            SELECT p FROM Person p
            WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
                OR p.documentNumber LIKE CONCAT('%', :term, '%'))
            """)
    Page<Person> search(@Param("term") String term, Pageable pageable);
}
