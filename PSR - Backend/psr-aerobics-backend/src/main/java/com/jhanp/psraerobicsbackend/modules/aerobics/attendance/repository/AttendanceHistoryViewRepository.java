package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.view.AttendanceHistoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;

/**
 * Solo lectura sobre vista_historial_asistencias: asistencia, persona, quién la marcó y con qué
 * método de pago, ya resueltos. No reconstruir estos JOIN a mano.
 * <p>
 * Los tres filtros del endpoint (persona, sesión, rango de fechas) van paginados: el historial solo
 * crece.
 */
public interface AttendanceHistoryViewRepository extends Repository<AttendanceHistoryView, Integer> {

    Page<AttendanceHistoryView> findByPersonIdOrderByDateDesc(Integer personId, Pageable pageable);

    Page<AttendanceHistoryView> findBySessionIdOrderByArrivalTimeAsc(Integer sessionId, Pageable pageable);

    Page<AttendanceHistoryView> findByDateBetweenOrderByDateDesc(LocalDate from,
                                                                 LocalDate to,
                                                                 Pageable pageable);
}
