package com.jhanp.psraerobicsbackend.modules.aerobics.session.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.view.SessionSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Solo lectura sobre vista_resumen_sesiones: una fila por sesión con el profesor resuelto y los
 * totales de llegadas y atendidos ya contados.
 */
public interface SessionSummaryViewRepository extends Repository<SessionSummaryView, Integer> {

    Optional<SessionSummaryView> findBySessionId(Integer sessionId);

    List<SessionSummaryView> findByDateOrderByStartTimeDesc(LocalDate date);

    Page<SessionSummaryView> findByDateBetweenOrderByDateDesc(LocalDate from,
                                                              LocalDate to,
                                                              Pageable pageable);
}
