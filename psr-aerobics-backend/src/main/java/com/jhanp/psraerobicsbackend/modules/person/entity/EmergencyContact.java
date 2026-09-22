package com.jhanp.psraerobicsbackend.modules.person.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "contacto_emergencia")
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ManyToOne, no OneToOne: hasta cinco contactos comparten persona. El {@code unique = true} que
     * llevaba antes esta columna era el que imponía la relación 1:1.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Person person;

    @Column(name = "nombre", length = 150)
    private String name;

    @Column(name = "telefono", length = 20)
    private String phone;

    // observations se fue a Person.healthObservations: es un dato de la persona, no del contacto.

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = OffsetDateTime.now();
    }
}
