package com.jhanp.psraerobicsbackend.modules.aerobics.report.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.view.SessionRevenueView;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Solo lectura sobre vista_recaudo_por_sesion: recaudo de cada sesión, para los días con más de una.
 * Solo cuenta movimientos ligados a una llegada.
 */
public interface SessionRevenueViewRepository extends Repository<SessionRevenueView, Integer> {

    Optional<SessionRevenueView> findBySessionId(Integer sessionId);

    List<SessionRevenueView> findByDateOrderBySessionIdAsc(LocalDate date);

    List<SessionRevenueView> findByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);
}
