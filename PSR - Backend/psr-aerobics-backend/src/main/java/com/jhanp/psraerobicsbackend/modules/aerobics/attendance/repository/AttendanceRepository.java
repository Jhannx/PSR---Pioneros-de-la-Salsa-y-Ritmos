package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    Optional<Attendance> findByCheckinId(Integer checkinId);

    boolean existsByCheckinId(Integer checkinId);

    List<Attendance> findByCheckinSessionId(Integer sessionId);

    Page<Attendance> findByCheckinPersonIdOrderByMarkedAtDesc(Integer personId, Pageable pageable);

    long countByCheckinSessionDate(LocalDate date);
}
