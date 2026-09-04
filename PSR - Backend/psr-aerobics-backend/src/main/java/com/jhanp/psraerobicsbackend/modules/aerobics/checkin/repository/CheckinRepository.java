package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity.Checkin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, Integer> {

    Optional<Checkin> findByPersonIdAndSessionId(Integer personId, Integer sessionId);

    boolean existsByPersonIdAndSessionId(Integer personId, Integer sessionId);

    List<Checkin> findBySessionIdOrderByArrivalTimeAsc(Integer sessionId);

    Page<Checkin> findByPersonIdOrderByArrivalTimeDesc(Integer personId, Pageable pageable);

    List<Checkin> findBySessionDateOrderByArrivalTimeAsc(LocalDate date);
}
