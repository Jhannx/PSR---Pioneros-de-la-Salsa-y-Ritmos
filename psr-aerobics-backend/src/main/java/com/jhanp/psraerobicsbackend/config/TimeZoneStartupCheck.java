package com.jhanp.psraerobicsbackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TimeZone;

/**
 * Deja en el log de arranque la zona horaria que ve Java y la que ve PostgreSQL <b>en una conexión
 * de la app</b>.
 * <p>
 * La del SQL Editor de Neon no sirve para esto: Postgres usa la zona que le manda cada cliente al
 * conectarse, y el editor manda la suya. Lo que decide el check-in es la zona de las conexiones del
 * pool, porque {@code fn_horario_vigente_ahora} y {@code fn_sesion_abierta_ahora} usan
 * {@code now()::time} y {@code CURRENT_DATE}. Si las dos líneas del log no coinciden, el check-in
 * resuelve la sesión con otra hora.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeZoneStartupCheck implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Zona Java: {} | ahora Java: {}", TimeZone.getDefault().getID(), ZonedDateTime.now());

        Map<String, Object> db = jdbcTemplate.queryForMap(
                "SELECT current_setting('TimeZone') AS zona, now()::text AS ahora, CURRENT_DATE::text AS hoy");
        String zona = (String) db.get("zona");

        if ("America/Bogota".equals(zona)) {
            log.info("Zona PostgreSQL: {} | ahora BD: {} | hoy BD: {}", zona, db.get("ahora"), db.get("hoy"));
        } else {
            log.warn("Zona PostgreSQL: {} (se esperaba America/Bogota) | ahora BD: {} | hoy BD: {} — el check-in"
                    + " resolverá la sesión con otra hora", zona, db.get("ahora"), db.get("hoy"));
        }
    }
}
