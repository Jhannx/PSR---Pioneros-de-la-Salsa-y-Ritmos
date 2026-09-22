package com.jhanp.psraerobicsbackend.modules.person.view;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Entity
@Immutable
@Table(name = "vista_perfil_persona")
public class PersonProfileView {

    @Id
    @Column(name = "persona_id")
    private Integer personId;

    @Column(name = "nombre_completo")
    private String fullName;

    /**
     * DocumentType y no String, PERO el que hace el trabajo es {@code @JdbcTypeCode}, no el
     * {@code columnDefinition}. Esa distinción costó un 500 en producción y conviene dejarla escrita.
     * <p>
     * Al retipar este campo de String a DocumentType se le puso solo el columnDefinition, copiando lo
     * que se había hecho en las vistas de CheckinMethod. Pero el columnDefinition es metadato de DDL:
     * con {@code ddl-auto=none} no se aplica nunca y, sobre todo, NO elige el tipo JDBC de lectura.
     * Aquellas vistas funcionaban porque CheckinMethodConverter (autoApply) estaba debajo haciendo la
     * conversión de verdad; para DocumentType no existe ningún converter.
     * <p>
     * Sin converter, sin {@code @Enumerated} y sin {@code @JdbcTypeCode}, el valor por defecto de JPA
     * para un enum es ORDINAL: Hibernate elegía TinyIntJdbcType, intentaba leer la columna como byte y
     * Postgres le devolvía 'CC'. De ahí el "Bad value for type byte : CC" que tumbaba
     * GET /api/personas/{id}/perfil y, con él, cualquier pantalla que lea vista_perfil_persona.
     * <p>
     * NAMED_ENUM es el mismo mecanismo que ya usa la entidad Person para esta misma columna.
     */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_documento", columnDefinition = "tipo_documento_enum")
    private DocumentType documentType;

    @Column(name = "numero_documento")
    private String documentNumber;

    @Column(name = "telefono")
    private String phone;

    @Column(name = "activo")
    private Boolean active;

    @Column(name = "fecha_nacimiento")
    private LocalDate birthDate;

    /**
     * Los tres campos de contacto de emergencia que había aquí
     * (contacto_emergencia_nombre/_telefono/observaciones_emergencia) salieron de la vista al pasar
     * los contactos a 1:N: el JOIN habría devuelto esta misma fila una vez por contacto, duplicando
     * de paso el saldo y el conteo de asistencias. La lista se pide a
     * GET /api/personas/{id}/contactos-emergencia.
     * <p>
     * Las observaciones sí siguen aquí, pero ahora vienen de persona.observaciones_salud: son una
     * sola por persona y no multiplican nada.
     */
    @Column(name = "observaciones_salud")
    private String healthObservations;

    @Column(name = "saldo")
    private BigDecimal balance;

    @Column(name = "dias_credito_disponible")
    private Integer availableCreditDays;

    @Column(name = "total_asistencias")
    private Long totalAttendances;

    @Column(name = "ultima_asistencia_fecha")
    private LocalDate lastAttendanceDate;

    @Column(name = "ultima_asistencia_hora")
    private OffsetDateTime lastAttendanceTime;
}
