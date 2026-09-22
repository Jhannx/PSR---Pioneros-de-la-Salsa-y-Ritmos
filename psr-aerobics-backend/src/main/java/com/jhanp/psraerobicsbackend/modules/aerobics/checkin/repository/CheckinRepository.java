package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, Integer> {

    Optional<Checkin> findByPersonIdAndSessionId(Integer personId, Integer sessionId);

    /** Si alguien ya se apunto a esa sesion. Decide si se le puede cambiar la hora. */
    boolean existsBySessionId(Integer sessionId);

    boolean existsByPersonIdAndSessionId(Integer personId, Integer sessionId);

    List<Checkin> findBySessionIdOrderByArrivalTimeAsc(Integer sessionId);

    Page<Checkin> findByPersonIdOrderByArrivalTimeDesc(Integer personId, Pageable pageable);

    List<Checkin> findBySessionDateOrderByArrivalTimeAsc(LocalDate date);

    /** Los asistentes de una sesion, con la persona ya resuelta para no pedirla fila por fila. */
    @Query("""
            SELECT c FROM Checkin c
            JOIN FETCH c.person
            WHERE c.session.id = :sessionId
            ORDER BY c.arrivalTime
            """)
    List<Checkin> findBySessionIdWithPerson(@Param("sessionId") Integer sessionId);
}
