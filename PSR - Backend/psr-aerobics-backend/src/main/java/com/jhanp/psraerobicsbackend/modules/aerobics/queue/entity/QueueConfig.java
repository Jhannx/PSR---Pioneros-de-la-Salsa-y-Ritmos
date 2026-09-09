package com.jhanp.psraerobicsbackend.modules.aerobics.queue.entity;

import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "configuracion_cola")
public class QueueConfig {

    @Id
    private Integer id;

    @Column(name = "personas_por_turno", nullable = false)
    private Integer personsPerTurn;

    @Column(name = "minutos_margen_ingreso", nullable = false)
    private Integer entryMarginMinutes;

    @Column(name = "minutos_visibilidad_pagos", nullable = false)
    private Integer paymentVisibilityMinutes;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User updatedBy;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = OffsetDateTime.now();
    }
}
