package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.entity;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity.Checkin;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "asistencia")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llegada_id", nullable = false, unique = true)
    private Checkin checkin;

    @Column(name = "monto_cobrado", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountCharged;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marcado_por", nullable = false)
    private User markedBy;

    @Column(name = "hora_marcado", nullable = false, updatable = false)
    private OffsetDateTime markedAt;

    @PrePersist
    protected void onCreate() {
        this.markedAt = OffsetDateTime.now();
    }
}
