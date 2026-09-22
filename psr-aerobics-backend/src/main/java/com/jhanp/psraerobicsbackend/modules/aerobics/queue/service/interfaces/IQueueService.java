package com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PrepaidQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.QueueSummaryResponse;
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

    /**
     * Solo la cola de la sesion vigente: la que esta en curso, o la proxima, o la ultima del dia.
     * <p>
     * Es lo que responde {@code GET /api/cola/admin} por defecto desde la ronda del 20-sep. Cual es la
     * vigente lo decide el servidor y no el cliente comparando horas: el reloj del navegador no es el
     * del servidor, y dos pantallas podian estar mostrando cosas distintas.
     */
    List<TodayQueueViewResponse> getCurrentSessionQueue();

    /**
     * Si hay clase en curso y cual es la siguiente de hoy, para el encabezado de la pantalla de cola.
     * <p>
     * Se responde aunque la cola venga vacia: es justo cuando hace falta poder decir a que hora
     * empieza la proxima.
     */
    /**
     * @param includeMoney false para un profesor: recibe la sesion pero las dos cifras en null. Ver
     *                     dinero es del mostrador, y quien dicta no tiene por que.
     */
    QueueSummaryResponse getSummary(boolean includeMoney);

    List<TodayQueueViewResponse> getTodayQueueBySession(Integer sessionId);

    /**
     * Pendientes de atender en una sesión.
     */
    List<TodayQueueViewResponse> getPendingBySession(Integer sessionId);

    /**
     * Pantalla de la sala de espera: sin datos de dinero. Filtrada por la sesion vigente (en curso
     * o la proxima de hoy); si no hay ninguna viva, devuelve vacio.
     */
    List<WaitingQueueViewResponse> getWaitingQueue();

    /**
     * Pantalla de la sala de espera, ya pagados. Filtrada por la sesion vigente. Incluye tanto a
     * quienes aun no fueron confirmados (la vista original) como a quienes fueron confirmados
     * automaticamente al llegar con credito, dentro de la ventana {@code paymentVisibilityMinutes}.
     */
    List<PrepaidQueueViewResponse> getPrepaidQueue();
}
