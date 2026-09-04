package com.jhanp.psraerobicsbackend.modules.aerobics.session.entity;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "sesion")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id")
    private Schedule schedule;

    @Column(name = "fecha", nullable = false)
    private LocalDate date;

    @Column(name = "hora_inicio", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "hora_fin")
    private OffsetDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profesor_usuario_id")
    private User teacher;

    @Column(name = "cancelada", nullable = false)
    private Boolean cancelled;

    @PrePersist
    protected void onCreate() {
        if (this.cancelled == null) {
            this.cancelled = false;
        }
    }
}
