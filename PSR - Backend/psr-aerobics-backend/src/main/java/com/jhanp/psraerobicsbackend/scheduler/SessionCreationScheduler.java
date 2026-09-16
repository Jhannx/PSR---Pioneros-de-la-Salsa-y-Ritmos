package com.jhanp.psraerobicsbackend.scheduler;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository.ScheduleExceptionRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository.ScheduleRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Crea por adelantado las sesiones de la semana a partir de los horarios activos.
 * <p>
 * Hasta ahora una sesión solo nacía cuando llegaba la primera persona: sp_registrar_llegada llama a
 * fn_obtener_o_crear_sesion_del_momento(). Eso funciona, pero significa que antes del primer check-in
 * la pantalla de sesiones está vacía aunque haya horario configurado, y para quien administra eso se
 * ve exactamente igual que "el horario no funciona". Decisión del 11-sep: se generan por adelantado.
 * <p>
 * Convive sin conflicto con el camino de check-in. Si la sesión ya existe —porque la creó este
 * barrido— fn_obtener_o_crear_sesion_del_momento la encuentra y la reutiliza en vez de crear otra.
 *
 * @see SessionClosingScheduler la otra mitad: cierra las que se quedaron abiertas
 */
@Component
public class SessionCreationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionCreationScheduler.class);

    /**
     * Cuántos días por delante se generan, contando hoy. Una semana: suficiente para que la pantalla
     * nunca se vea vacía y corto como para que reconfigurar un horario no obligue a borrar sesiones
     * de dentro de un mes.
     */
    private static final int HORIZON_DAYS = 7;

    private final ScheduleRepository scheduleRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final SessionRepository sessionRepository;
    private final SessionProcedureRepository sessionProcedureRepository;

    public SessionCreationScheduler(ScheduleRepository scheduleRepository,
                                    ScheduleExceptionRepository scheduleExceptionRepository,
                                    SessionRepository sessionRepository,
                                    SessionProcedureRepository sessionProcedureRepository) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleExceptionRepository = scheduleExceptionRepository;
        this.sessionRepository = sessionRepository;
        this.sessionProcedureRepository = sessionProcedureRepository;
    }

    /**
     * Una pasada al arrancar (tras un minuto, para no competir con Flyway ni con el seed) y otra cada
     * seis horas. No hace falta más: la ventana avanza un día por día, y repetir la pasada es barato
     * porque todo lo que ya existe se salta.
     * <p>
     * fixedDelay y no fixedRate, igual que en el cierre: si una pasada se demora, la siguiente espera
     * en vez de solaparse.
     */
    @Scheduled(fixedDelayString = "PT6H", initialDelayString = "PT1M")
    public void createUpcomingSessions() {
        try {
            LocalDate today = LocalDate.now();
            int created = 0;

            for (Schedule schedule : scheduleRepository.findByActiveTrueOrderByDayOfWeekAscStartTimeAsc()) {
                for (int offset = 0; offset < HORIZON_DAYS; offset++) {
                    if (createIfMissing(schedule, today.plusDays(offset))) {
                        created++;
                    }
                }
            }

            if (created > 0) {
                log.info("Creación anticipada de sesiones: {} sesión(es) creada(s) para los próximos {} días",
                        created, HORIZON_DAYS);
            }
        } catch (Exception e) {
            // Se traga la excepción a propósito, igual que el scheduler de cierre: si falla una
            // pasada, el scheduler tiene que seguir vivo para intentarlo en la siguiente.
            log.error("Falló la creación anticipada de sesiones", e);
        }
    }

    /**
     * @return true si creó la sesión; false si no tocaba, ya existía o falló solo esta.
     */
    private boolean createIfMissing(Schedule schedule, LocalDate date) {
        // horario.dia_semana es ISO (1 = lunes ... 7 = domingo), documentado en V1, y
        // DayOfWeek.getValue() de Java usa exactamente esa numeración. No hay conversión que hacer —
        // y conviene no "arreglarlo": EXTRACT(DOW) de Postgres sí está desplazado (0 = domingo), y es
        // justo el error que dejaría todas las clases corridas un día.
        if (date.getDayOfWeek().getValue() != schedule.getDayOfWeek()) {
            return false;
        }

        // Día marcado como excepción: ese horario no aplica esa fecha. Sin esto, el barrido repondría
        // las clases que alguien canceló a propósito.
        if (scheduleExceptionRepository.existsByScheduleIdAndDate(schedule.getId(), date)) {
            return false;
        }

        // Cuenta también las canceladas: si un admin canceló la clase generada, no se recrea.
        if (sessionRepository.existsByScheduleIdAndDate(schedule.getId(), date)) {
            return false;
        }

        try {
            // horario guarda TIME (sin zona) y sesion guarda TIMESTAMPTZ, así que hay que anclar la
            // hora a una fecha y a una zona. Se usa la zona del sistema, la misma con la que
            // SessionService construye sus horas y con la que Postgres evalúa now().
            ZoneId zone = ZoneId.systemDefault();
            OffsetDateTime startTime = date.atTime(schedule.getStartTime()).atZone(zone).toOffsetDateTime();
            OffsetDateTime endTime = date.atTime(schedule.getEndTime()).atZone(zone).toOffsetDateTime();

            // Sin profesor: se asigna después con PUT /api/sesiones/{id}/profesor/{userId}. El
            // horario no guarda quién dicta, así que no hay nada que heredar.
            sessionProcedureRepository.createManualSession(date, startTime, endTime, null, schedule.getId());
            return true;

        } catch (Exception e) {
            // Una sesión que falla no puede llevarse por delante el resto de la semana. El caso
            // esperable es que otra pasada la haya creado entre el existsBy y el INSERT, y entonces
            // el procedimiento avisa de que ya existe: nada que hacer, se sigue.
            log.warn("No se pudo crear la sesión del horario {} para el {}: {}",
                    schedule.getId(), date, e.getMessage());
            return false;
        }
    }
}
