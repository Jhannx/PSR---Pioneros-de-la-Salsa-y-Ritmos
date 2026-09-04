package com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PrepaidQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.TodayQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.WaitingQueueViewResponse;

import java.util.List;

/**
 * Lectura de la cola del día. Va aparte de IQueueConfigService, que administra los parámetros de la
 * cola: son dos responsabilidades distintas sobre el mismo módulo.
 * <p>
 * Sin paginación en ninguno de los cinco métodos, y es a propósito: las tres vistas ya están
 * acotadas al día de hoy, y una fila de aeróbicos no llega a decenas de personas. Paginar la
 * pantalla de la sala de espera además la rompería — tiene que mostrarse completa.
 */
public interface IQueueService {

    /**
     * Cola completa del día para el panel del admin: incluye montos, saldo y nota.
     */
    List<TodayQueueViewResponse> getTodayQueue();

    List<TodayQueueViewResponse> getTodayQueueBySession(Integer sessionId);

    /**
     * Pendientes de atender en una sesión.
     */
    List<TodayQueueViewResponse> getPendingBySession(Integer sessionId);

    /**
     * Pantalla de la sala de espera: sin datos de dinero.
     */
    List<WaitingQueueViewResponse> getWaitingQueue();

    /**
     * Pantalla de la sala de espera, ya pagados. Solo cuenta el pago hecho antes de la llegada, y
     * cada fila desaparece al vencer minutos_visibilidad_pagos.
     */
    List<PrepaidQueueViewResponse> getPrepaidQueue();
}
