package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "horario_excepcion")
public class ScheduleException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id", nullable = false)
    private Schedule schedule;

    @Column(name = "fecha", nullable = false)
    private LocalDate date;

    @Column(name = "motivo")
    private String reason;
}
