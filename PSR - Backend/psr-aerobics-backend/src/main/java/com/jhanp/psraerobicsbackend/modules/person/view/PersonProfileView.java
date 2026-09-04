package com.jhanp.psraerobicsbackend.modules.person.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

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

    @Column(name = "tipo_documento")
    private String documentType;

    @Column(name = "numero_documento")
    private String documentNumber;

    @Column(name = "telefono")
    private String phone;

    @Column(name = "activo")
    private Boolean active;

    @Column(name = "contacto_emergencia_nombre")
    private String emergencyContactName;

    @Column(name = "contacto_emergencia_telefono")
    private String emergencyContactPhone;

    @Column(name = "observaciones_emergencia")
    private String emergencyObservations;

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
