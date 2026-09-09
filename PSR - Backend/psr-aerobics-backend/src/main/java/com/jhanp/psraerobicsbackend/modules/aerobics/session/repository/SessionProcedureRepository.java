package com.jhanp.psraerobicsbackend.modules.aerobics.session.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Acceso a los procedimientos de sesión. Solo cubren la creación manual por parte del admin: la
 * sesión del flujo normal de check-in la crea la base de datos desde sp_registrar_llegada.
 */
@Repository
public class SessionProcedureRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall createManualSessionCall;
    private final SimpleJdbcCall closeExpiredSessionsCall;

    public SessionProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.closeExpiredSessionsCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_cerrar_sesiones_vencidas")
                .withoutProcedureColumnMetaDataAccess()
                .withNamedBinding()
                .declareParameters(
                        new SqlParameter("p_horas_gracia", Types.INTEGER),
                        new SqlOutParameter("p_cerradas", Types.INTEGER)
                );
        this.createManualSessionCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_crear_sesion_manual")
                .withoutProcedureColumnMetaDataAccess()
                .withNamedBinding()
                .declareParameters(
                        new SqlParameter("p_fecha", Types.DATE),
                        new SqlParameter("p_hora_inicio", Types.TIMESTAMP_WITH_TIMEZONE),
                        new SqlParameter("p_hora_fin", Types.TIMESTAMP_WITH_TIMEZONE),
                        new SqlParameter("p_profesor_usuario_id", Types.INTEGER),
                        new SqlParameter("p_horario_id", Types.INTEGER),
                        new SqlOutParameter("p_sesion_id", Types.INTEGER)
                );
    }

    /**
     * sp_crear_sesion_manual. Falla si ya existe una sesión para ese horario y fecha, o si el
     * usuario indicado como profesor no tiene el rol PROFESOR. scheduleId y teacherUserId admiten null.
     */
    public Integer createManualSession(LocalDate date,
                                       OffsetDateTime startTime,
                                       OffsetDateTime endTime,
                                       Integer teacherUserId,
                                       Integer scheduleId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_fecha", date)
                .addValue("p_hora_inicio", startTime)
                .addValue("p_hora_fin", endTime)
                .addValue("p_profesor_usuario_id", teacherUserId)
                .addValue("p_horario_id", scheduleId);

        return (Integer) createManualSessionCall.execute(params).get("p_sesion_id");
    }

    /**
     * sp_cancelar_sesion. Audita por su cuenta con el usuario recibido, sin set_config previo.
     * Falla si la sesión no existe o ya estaba cancelada.
     */
    public void cancel(Integer sessionId, Integer executedByUserId) {
        jdbcTemplate.update("CALL sp_cancelar_sesion(?, ?)", sessionId, executedByUserId);
    }

    /**
     * sp_cerrar_sesiones_vencidas. Cierra las sesiones abiertas pasadas las horas de gracia y las
     * audita a nombre del usuario SISTEMA. Devuelve cuántas cerró.
     */
    public Integer closeExpiredSessions(Integer graceHours) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_horas_gracia", graceHours);

        return (Integer) closeExpiredSessionsCall.execute(params).get("p_cerradas");
    }
}
