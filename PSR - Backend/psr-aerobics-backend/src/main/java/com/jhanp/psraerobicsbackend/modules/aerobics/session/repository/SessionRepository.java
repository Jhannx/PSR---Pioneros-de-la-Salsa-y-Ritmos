package com.jhanp.psraerobicsbackend.modules.aerobics.session.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {

    List<Session> findByDateOrderByStartTimeAsc(LocalDate date);

    /**
     * Rango de fechas: paginado, porque un rango amplio devuelve una sesión por día o más.
     * El filtro por fecha puntual no lo necesita — un día tiene una o dos.
     */
    Page<Session> findByDateBetweenOrderByDateDescStartTimeDesc(LocalDate from, LocalDate to, Pageable pageable);

    Page<Session> findAllByOrderByDateDescStartTimeDesc(Pageable pageable);

    Optional<Session> findByScheduleIdAndDate(Integer scheduleId, LocalDate date);

    /**
     * Para SessionCreationScheduler, que solo necesita saber si ya existe y no la fila entera.
     * <p>
     * Cuenta también las canceladas, y es intencionado: si un admin cancela una clase generada, esto
     * es lo que impide que el siguiente barrido se la vuelva a crear.
     */
    boolean existsByScheduleIdAndDate(Integer scheduleId, LocalDate date);

    /**
     * Sesión del día todavía en curso: sin hora de fin y no cancelada.
     */
    Optional<Session> findByDateAndEndTimeIsNullAndCancelledFalse(LocalDate date);

    List<Session> findByTeacherIdOrderByDateDesc(Integer teacherUserId);
}
