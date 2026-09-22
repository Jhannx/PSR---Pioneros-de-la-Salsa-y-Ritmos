package com.jhanp.psraerobicsbackend.modules.person.repository;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;

/**
 * Acceso a los procedimientos almacenados de persona. Se mantiene aparte de {@link PersonRepository}
 * porque los procedimientos con parámetros OUT requieren SimpleJdbcCall y no encajan en una interfaz
 * de Spring Data.
 */
@Repository
public class PersonProcedureRepository {

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall registerPersonCall;

    public PersonProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.registerPersonCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_registrar_persona")
                .withoutProcedureColumnMetaDataAccess()
                .withNamedBinding()
                .declareParameters(
                        new SqlParameter("p_tipo_documento", Types.OTHER),
                        new SqlParameter("p_numero_documento", Types.VARCHAR),
                        new SqlParameter("p_nombre", Types.VARCHAR),
                        new SqlParameter("p_apellidos", Types.VARCHAR),
                        new SqlParameter("p_telefono", Types.VARCHAR),
                        new SqlParameter("p_fecha_nacimiento", Types.DATE),
                        new SqlParameter("p_fuente_registro", Types.OTHER),
                        new SqlOutParameter("p_persona_id", Types.INTEGER)
                );
    }

    /**
     * sp_registrar_persona. Los enums de Postgres se envían como Types.OTHER; si el driver rechazara
     * el cast, hay que envolver el valor en un PGobject con el nombre del tipo.
     * <p>
     * El orden de los parámetros declarados arriba tiene que coincidir con el del procedimiento en
     * R__STORED_PROCEDURES.sql. Se declaran a mano
     * ({@code withoutProcedureColumnMetaDataAccess}), así que si el procedimiento cambia de firma y
     * esto no, el fallo aparece en tiempo de ejecución y no al compilar.
     *
     * @param birthDate admite null: la columna es nullable para no romper a las personas ya
     *                  registradas. Que sea obligatorio en las altas nuevas lo valida PersonRequest.
     */
    public Integer registerPerson(DocumentType documentType,
                                  String documentNumber,
                                  String name,
                                  String lastName,
                                  String phone,
                                  LocalDate birthDate,
                                  RegistrationSource registrationSource) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("p_tipo_documento", documentType.name(), Types.OTHER)
                .addValue("p_numero_documento", documentNumber)
                .addValue("p_nombre", name)
                .addValue("p_apellidos", lastName)
                .addValue("p_telefono", phone)
                .addValue("p_fecha_nacimiento", birthDate, Types.DATE)
                .addValue("p_fuente_registro", registrationSource.getDbValue(), Types.OTHER);

        return (Integer) registerPersonCall.execute(params).get("p_persona_id");
    }

    /**
     * sp_reactivar_persona. El procedimiento audita por su cuenta con el usuario recibido,
     * así que no requiere set_config previo.
     */
    public void reactivate(Integer personId, Integer executedByUserId) {
        jdbcTemplate.update("CALL sp_reactivar_persona(?, ?)", personId, executedByUserId);
    }

    /**
     * sp_eliminar_persona. Irreversible: borra la persona y su cuenta de acceso, después de reasignar
     * su huella (llegadas, movimientos, asistencias, auditoría) a la persona/usuario sentinel.
     * El procedimiento exige force = true y fija app.usuario_id por su cuenta.
     */
    public void deletePermanently(Integer personId, Integer executedByUserId) {
        jdbcTemplate.update("CALL sp_eliminar_persona(?, ?, ?)", personId, executedByUserId, true);
    }
}
