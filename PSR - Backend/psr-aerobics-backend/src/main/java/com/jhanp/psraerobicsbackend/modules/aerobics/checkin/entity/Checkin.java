package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "llegada")
public class Checkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private Session session;

    @Column(name = "hora_llegada", nullable = false)
    private OffsetDateTime arrivalTime;

    // Sin @Enumerated: CheckinMethodConverter se aplica solo y guarda el valor en español.
    @Column(name = "metodo_checkin", nullable = false, columnDefinition = "metodo_checkin_enum")
    private CheckinMethod checkinMethod;

    @PrePersist
    protected void onCreate() {
        if (this.arrivalTime == null) {
            this.arrivalTime = OffsetDateTime.now();
        }
    }
}
