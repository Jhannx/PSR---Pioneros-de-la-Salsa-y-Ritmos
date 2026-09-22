package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    /**
     * Lo que ha cobrado una sesion: la suma de sus asistencias confirmadas.
     * <p>
     * Es devengado — lo que la clase genero— y no caja: un adelanto pagado hoy para el miercoles no
     * cuenta aqui, cuenta el miercoles cuando se confirme la asistencia. La cifra de caja del dia es
     * {@code AccountMovementRepository.findCollectedToday}.
     */
    @Query("SELECT COALESCE(SUM(a.amountCharged), 0) FROM Attendance a WHERE a.checkin.session.id = :sessionId")
    BigDecimal sumChargedBySession(@Param("sessionId") Integer sessionId);
}
