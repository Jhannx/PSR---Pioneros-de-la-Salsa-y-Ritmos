package com.jhanp.psraerobicsbackend.modules.person.entity;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "persona")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", columnDefinition = "tipo_documento_enum")
    private DocumentType documentType;

    @Column(name = "numero_documento", unique = true, length = 20)
    private String documentNumber;

    @Column(name = "nombre", length = 100)
    private String name;

    @Column(name = "apellidos", length = 100)
    private String lastName;

    @Column(name = "telefono", length = 20)
    private String phone;

    // Sin @Enumerated: RegistrationSourceConverter se aplica solo y guarda el valor en español.
    @Column(name = "fuente_registro", nullable = false, columnDefinition = "fuente_registro_enum")
    private RegistrationSource registrationSource;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "activo", nullable = false)
    private Boolean active;

    @OneToOne(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private EmergencyContact emergencyContact;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = OffsetDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }
}
