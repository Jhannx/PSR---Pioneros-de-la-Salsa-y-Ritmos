package com.jhanp.psraerobicsbackend.modules.aerobics.queue.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.PrepaidQueueView;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Solo lectura sobre vista_fila_prepagada: quienes ya pagaron antes de llegar y pueden entrar
 * directo. La vista aplica sola la ventana de minutos_visibilidad_pagos, así que una fila
 * desaparece de aquí al vencer ese tiempo.
 */
public interface PrepaidQueueViewRepository extends Repository<PrepaidQueueView, Integer> {

    List<PrepaidQueueView> findAllByOrderByArrivalTimeAsc();
}
