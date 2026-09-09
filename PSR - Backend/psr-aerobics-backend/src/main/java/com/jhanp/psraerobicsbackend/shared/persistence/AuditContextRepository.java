package com.jhanp.psraerobicsbackend.shared.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Publica el usuario autenticado en la variable de sesión app.usuario_id, que leen los triggers de
 * auditoría a través de fn_usuario_actual().
 * <p>
 * Debe invocarse dentro de la misma transacción, antes de:
 * <ul>
 *   <li>cualquier UPDATE sobre movimiento_cuenta (incluido sp_entregar_vuelto),</li>
 *   <li>correcciones del monto cobrado de una asistencia,</li>
 *   <li>UPDATE de tarifa o de configuracion_cola.</li>
 * </ul>
 * Los procedimientos que auditan por su cuenta (sp_reactivar_persona, sp_cancelar_sesion,
 * sp_eliminar_persona) reciben el usuario por parámetro y no lo necesitan.
 */
@Repository
public class AuditContextRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditContextRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * El tercer argumento en true limita el valor a la transacción actual.
     * <p>
     * Se consulta con queryForObject y no con update(): set_config devuelve una fila, y el driver de
     * Postgres rechaza executeUpdate() sobre una sentencia que produce resultados
     * ("A result was returned when none was expected").
     */
    public void setCurrentUser(Integer userId) {
        jdbcTemplate.queryForObject("SELECT set_config('app.usuario_id', ?, true)",
                String.class, userId.toString());
    }
}
