package com.jhanp.psraerobicsbackend.modules.role.repository;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a los procedimientos de roles. Reciben el nombre del rol como enum de Postgres, no el id,
 * de ahí el CAST explícito: el driver envía el String como varchar y no hay conversión implícita.
 */
@Repository
public class UserRoleProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRoleProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * sp_asignar_rol. Idempotente gracias a uq_usuario_rol.
     * trg_validar_persona_miembro rechaza asignar MIEMBRO a un usuario sin persona asociada.
     */
    public void assign(Integer userId, RoleName roleName) {
        jdbcTemplate.update("CALL sp_asignar_rol(?, CAST(? AS rol_nombre_enum))",
                userId, roleName.getDbValue());
    }

    /**
     * sp_quitar_rol. Falla si dejaría al usuario sin ningún rol.
     */
    public void revoke(Integer userId, RoleName roleName) {
        jdbcTemplate.update("CALL sp_quitar_rol(?, CAST(? AS rol_nombre_enum))",
                userId, roleName.getDbValue());
    }
}
