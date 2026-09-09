package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a los procedimientos de asistencia. No existe alta por JPA: sp_registrar_asistencia cobra
 * la tarifa vigente en el mismo instante del INSERT, así que no debe replicarse con save().
 */
@Repository
public class AttendanceProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttendanceProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * sp_registrar_asistencia. Inserta, así que no dispara trg_auditar_asistencia (AFTER UPDATE)
     * y no necesita set_config previo. Falla si la llegada ya fue atendida.
     */
    public void confirm(Integer checkinId, Integer markedByUserId) {
        jdbcTemplate.update("CALL sp_registrar_asistencia(?, ?)", checkinId, markedByUserId);
    }

    /**
     * sp_anular_asistencia. Borra el registro; falla si esa llegada no tenía asistencia.
     */
    public void cancel(Integer checkinId) {
        jdbcTemplate.update("CALL sp_anular_asistencia(?)", checkinId);
    }
}
