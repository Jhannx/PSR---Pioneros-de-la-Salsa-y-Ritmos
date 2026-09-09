package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;

/**
 * Acceso a sp_registrar_llegada, única puerta de entrada al check-in: la sesión la resuelve la base
 * de datos con fn_obtener_o_crear_sesion_del_momento(), no la aplicación.
 */
@Repository
public class CheckinProcedureRepository {

    private final SimpleJdbcCall registerArrivalCall;

    public CheckinProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.registerArrivalCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_registrar_llegada")
                .withoutProcedureColumnMetaDataAccess()
                .withNamedBinding()
                .declareParameters(
                        new SqlParameter("p_persona_id", Types.INTEGER),
                        new SqlParameter("p_metodo_checkin", Types.OTHER),
                        new SqlOutParameter("p_llegada_id", Types.INTEGER)
                );
    }

    /**
     * Puede propagar SQLSTATE 75001 (persona desactivada) y 75002 (no hay clase vigente).
     * Son dos mensajes distintos para el usuario final; se traducen en el manejador global.
     */
    public Integer registerArrival(Integer personId, CheckinMethod checkinMethod) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_persona_id", personId)
                .addValue("p_metodo_checkin", checkinMethod.getDbValue(), Types.OTHER);

        return (Integer) registerArrivalCall.execute(params).get("p_llegada_id");
    }
}
