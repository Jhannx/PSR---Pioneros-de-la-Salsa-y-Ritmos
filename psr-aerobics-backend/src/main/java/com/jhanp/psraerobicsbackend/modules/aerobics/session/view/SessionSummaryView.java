package com.jhanp.psraerobicsbackend.modules.aerobics.session.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Entity
@Immutable
@Table(name = "vista_resumen_sesiones")
public class SessionSummaryView {

    @Id
    @Column(name = "sesion_id")
    private Integer sessionId;

    @Column(name = "horario_id")
    private Integer scheduleId;

    @Column(name = "fecha")
    private LocalDate date;

    @Column(name = "hora_inicio")
    private OffsetDateTime startTime;

    @Column(name = "hora_fin")
    private OffsetDateTime endTime;

    @Column(name = "profesor_usuario_id")
    private Integer teacherUserId;

    @Column(name = "profesor_nombre")
    private String teacherName;

    @Column(name = "cancelada")
    private Boolean cancelled;

    @Column(name = "total_llegadas")
    private Long totalCheckins;

    @Column(name = "total_atendidos")
    private Long totalAttended;
}
