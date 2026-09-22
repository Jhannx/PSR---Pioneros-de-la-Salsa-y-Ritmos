package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "horario")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "dia_semana", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime startTime;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime endTime;

    @Column(name = "activo", nullable = false)
    private Boolean active;

    @PrePersist
    protected void onCreate() {
        if (this.active == null) {
            this.active = true;
        }
    }
}
