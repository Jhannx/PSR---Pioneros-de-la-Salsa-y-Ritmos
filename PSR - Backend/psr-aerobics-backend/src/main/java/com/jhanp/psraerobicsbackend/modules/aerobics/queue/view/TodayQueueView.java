package com.jhanp.psraerobicsbackend.modules.aerobics.queue.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Immutable
@Table(name = "vista_fila_hoy")
public class TodayQueueView {

    @Id
    @Column(name = "llegada_id")
    private Integer checkinId;

    @Column(name = "sesion_id")
    private Integer sessionId;

    @Column(name = "persona_id")
    private Integer personId;

    @Column(name = "nombre")
    private String firstName;

    @Column(name = "apellidos")
    private String lastName;

    @Column(name = "hora_llegada")
    private OffsetDateTime arrivalTime;

    @Column(name = "metodo_checkin")
    private String checkinMethod;

    @Column(name = "posicion")
    private Long position;

    @Column(name = "asistencia_id")
    private Integer attendanceId;

    @Column(name = "monto_cobrado")
    private BigDecimal amountCharged;

    @Column(name = "hora_marcado")
    private OffsetDateTime markedAt;

    @Column(name = "atendido")
    private Boolean attended;

    @Column(name = "saldo")
    private BigDecimal balance;

    @Column(name = "nota")
    private String note;
}
