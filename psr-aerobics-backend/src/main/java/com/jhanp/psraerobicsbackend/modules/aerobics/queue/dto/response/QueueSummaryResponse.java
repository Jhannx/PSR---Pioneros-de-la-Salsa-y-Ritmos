package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionResponse;

import java.math.BigDecimal;

/**
 * El encabezado de la pantalla de cola: que sesion esta vigente y cuanto se ha recaudado.
 * <p>
 * <b>La sesion viene completa y sale de la tabla de sesiones, no de las filas de la cola.</b> Esa es
 * la diferencia que importa: una clase en curso con cero llegadas hace que la cola responda
 * {@code []}, y entonces el cliente no tenia de donde sacar su id ni su hora de fin — ni para decir
 * "termina a las 11:50" ni para ofrecer finalizarla. Peor aun, al elegirla de un listado que podia
 * estar desactualizado, el boton "Finalizar sesion" llego a apuntar a una clase ya terminada.
 */
public record QueueSummaryResponse(

        /**
         * La sesion vigente, o {@code null} si hoy no hay ninguna.
         * <p>
         * "Vigente" en este orden: la que esta en curso; si no hay, la proxima de hoy; si no hay, la
         * ultima de hoy. La ultima entra aqui aunque la cola venga vacia — son dos preguntas
         * distintas: la cola dice a quien hay que atender ahora, y esto de que clase se esta hablando.
         */
        SessionResponse session,

        /**
         * Lo que ha cobrado <b>esta clase</b>: la suma de lo cobrado en sus asistencias confirmadas.
         * <p>
         * Es devengado, no caja: cuenta lo que la clase genero, sin importar cuando entro el dinero.
         * Un adelanto pagado hoy para el miercoles no cuenta aqui.
         * <p>
         * {@code null} para quien no tiene permiso de ver dinero — un profesor. El cliente no muestra
         * nada cuando viene asi.
         */
        BigDecimal collectedThisSession,

        /**
         * Todo lo abonado hoy, en todas las sesiones, adelantos incluidos y <b>restando reembolsos</b>.
         * <p>
         * Es caja del dia: cuenta por la fecha en que entro el dinero. El vuelto entregado no resta
         * porque nunca llego a abonarse — quien paga 5.000 con un billete de 7.000 abona 5.000, y los
         * 2.000 siguen siendo suyos.
         * <p>
         * {@code null} para quien no puede ver dinero.
         */
        BigDecimal collectedToday
) {
}
