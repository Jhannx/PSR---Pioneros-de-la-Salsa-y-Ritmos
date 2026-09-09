package com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.TodayQueueView;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Solo lectura sobre vista_fila_hoy: la cola completa del día para el panel del admin, con dinero
 * y saldo. La vista ya filtra por fecha de hoy y descarta sesiones canceladas.
 */
public interface TodayQueueViewRepository extends Repository<TodayQueueView, Integer> {

    java.util.Optional<TodayQueueView> findById(Integer checkinId);

    List<TodayQueueView> findAllByOrderByArrivalTimeAsc();

    List<TodayQueueView> findBySessionIdOrderByPositionAsc(Integer sessionId);

    /**
     * Pendientes de atender en una sesión.
     */
    List<TodayQueueView> findBySessionIdAndAttendedFalseOrderByPositionAsc(Integer sessionId);
}
