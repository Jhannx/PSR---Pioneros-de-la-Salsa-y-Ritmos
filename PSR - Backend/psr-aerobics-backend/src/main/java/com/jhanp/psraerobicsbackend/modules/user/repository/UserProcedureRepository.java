package com.jhanp.psraerobicsbackend.modules.user.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;

/**
 * Acceso a los procedimientos de usuario. No existe alta de MIEMBRO por JPA: la cuenta siempre nace
 * asociada a una persona ya registrada, en la misma transacción.
 */
@Repository
public class UserProcedureRepository {

    private final SimpleJdbcCall registerUserForPersonCall;

    public UserProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.registerUserForPersonCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_registrar_usuario_para_persona")
                .withoutProcedureColumnMetaDataAccess()
                .withNamedBinding()
                .declareParameters(
                        new SqlParameter("p_persona_id", Types.INTEGER),
                        new SqlParameter("p_password_hash", Types.VARCHAR),
                        new SqlOutParameter("p_usuario_id", Types.INTEGER)
                );
    }

    /**
     * sp_registrar_usuario_para_persona. Crea la cuenta y le asigna el rol MIEMBRO.
     * Recibe el hash ya calculado: la contraseña en claro nunca llega a la base de datos.
     * Deriva internamente el numero_documento de la persona asociada.
     */
    public Integer registerUserForPerson(Integer personId, String passwordHash) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_persona_id", personId)
                .addValue("p_password_hash", passwordHash);

        return (Integer) registerUserForPersonCall.execute(params).get("p_usuario_id");
    }
}
