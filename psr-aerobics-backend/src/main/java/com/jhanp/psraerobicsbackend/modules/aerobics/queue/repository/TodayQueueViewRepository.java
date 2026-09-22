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

    /**
     * La llegada de hoy de una persona, la mas reciente si hoy hubiera mas de una clase.
     * <p>
     * Es la unica lectura de cola que un MIEMBRO puede hacer, y solo de si mismo: el resto de esta
     * vista lleva dinero y saldo de todo el mundo.
     */
    java.util.Optional<TodayQueueView> findFirstByPersonIdOrderByArrivalTimeDesc(Integer personId);

    List<TodayQueueView> findBySessionIdOrderByPositionAsc(Integer sessionId);

    /**
     * Pendientes de atender en una sesión.
     */
    List<TodayQueueView> findBySessionIdAndAttendedFalseOrderByPositionAsc(Integer sessionId);
}
