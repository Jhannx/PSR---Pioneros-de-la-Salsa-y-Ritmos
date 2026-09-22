package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.event;

/**
 * Un horario se creó, se modificó o cambió de estado. Lo escucha el generador de sesiones para
 * materializar de inmediato lo que ese horario implica en los próximos días.
 * <p>
 * Un evento y no una llamada directa desde {@code ScheduleService} por dos razones:
 * <ul>
 *   <li><b>Se atiende después del commit.</b> El horario ya está guardado y visible cuando se generan
 *       sus sesiones; si la generación falla, el horario sigue creado. Al revés —dentro de la misma
 *       transacción— un fallo al insertar una sesión dejaría la transacción abortada y el horario no
 *       se guardaría, que es peor que el problema que se está arreglando.</li>
 *   <li>El módulo de horarios no tiene por qué saber que existe un scheduler.</li>
 * </ul>
 * Mismo patrón que los correos de auth, por los mismos motivos.
 */
public record ScheduleChangedEvent(Integer scheduleId) {
}
