package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Immutable
@Table(name = "vista_saldo_por_asistencia")
public class AttendanceBalanceView {

    @Id
    @Column(name = "asistencia_id")
    private Integer attendanceId;

    @Column(name = "persona_id")
    private Integer personId;

    @Column(name = "fecha")
    private LocalDate date;

    @Column(name = "monto_cobrado")
    private BigDecimal amountCharged;

    @Column(name = "saldo_despues_de_esta_asistencia")
    private BigDecimal balanceAfterThisAttendance;

    @Column(name = "estado_saldo")
    private String balanceStatus;

    @Column(name = "monto_pendiente")
    private BigDecimal pendingAmount;
}
