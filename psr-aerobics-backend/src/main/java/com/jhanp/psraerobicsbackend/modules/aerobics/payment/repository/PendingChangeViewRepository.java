package com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.view.PendingChangeView;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Solo lectura sobre vista_vueltos_pendientes: abonos donde se recibió más de lo abonado y el
 * vuelto todavía no se entregó. Se salda llamando a sp_entregar_vuelto.
 */
public interface PendingChangeViewRepository extends Repository<PendingChangeView, Integer> {

    List<PendingChangeView> findAllByOrderByDateTimeDesc();

    List<PendingChangeView> findByPersonIdOrderByDateTimeDesc(Integer personId);
}
