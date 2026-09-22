package com.jhanp.psraerobicsbackend.modules.aerobics.queue.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@Getter
@Entity
@Immutable
@Table(name = "vista_fila_prepagada")
public class PrepaidQueueView {

    @Id
    @Column(name = "llegada_id")
    private Integer checkinId;

    @Column(name = "nombre")
    private String firstName;

    @Column(name = "apellidos")
    private String lastName;

    @Column(name = "hora_llegada")
    private OffsetDateTime arrivalTime;
}
