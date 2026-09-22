package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.view;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Entity
@Immutable
@Table(name = "vista_historial_asistencias")
public class AttendanceHistoryView {

    @Id
    @Column(name = "asistencia_id")
    private Integer attendanceId;

    @Column(name = "llegada_id")
    private Integer checkinId;

    @Column(name = "sesion_id")
    private Integer sessionId;

    @Column(name = "persona_id")
    private Integer personId;

    @Column(name = "nombre_completo")
    private String fullName;

    @Column(name = "fecha")
    private LocalDate date;

    @Column(name = "hora_llegada")
    private OffsetDateTime arrivalTime;

    /**
     * CheckinMethod y no String: ver la nota larga en TodayQueueView. Mismo fallo, mismo origen —
     * MapStruct generaba {@code valueOf("DOCUMENTO")} contra constantes en inglés y tumbaba el
     * historial de asistencias entero.
     */
    @Column(name = "metodo_checkin", columnDefinition = "metodo_checkin_enum")
    private CheckinMethod checkinMethod;

    @Column(name = "monto_cobrado")
    private BigDecimal amountCharged;

    @Column(name = "hora_marcado")
    private OffsetDateTime markedAt;

    @Column(name = "marcado_por")
    private Integer markedByUserId;

    @Column(name = "marcado_por_nombre")
    private String markedByName;

    @Column(name = "metodo_pago")
    private String paymentMethod;
}
