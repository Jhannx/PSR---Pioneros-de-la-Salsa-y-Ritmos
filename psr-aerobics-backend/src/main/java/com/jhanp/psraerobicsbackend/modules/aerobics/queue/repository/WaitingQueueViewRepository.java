package com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.WaitingQueueView;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Solo lectura sobre vista_fila_espera: lo que se proyecta en la pantalla de la sala de espera.
 * Sin datos de dinero, por diseño de la vista.
 */
public interface WaitingQueueViewRepository extends Repository<WaitingQueueView, Integer> {

    List<WaitingQueueView> findAllByOrderByArrivalTimeAsc();

    List<WaitingQueueView> findBySessionIdOrderByPositionAsc(Integer sessionId);
}
