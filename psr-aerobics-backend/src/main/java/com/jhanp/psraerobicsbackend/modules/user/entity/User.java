package com.jhanp.psraerobicsbackend.modules.user.entity;

import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.role.entity.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "usuario")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_documento", unique = true, length = 20)
    private String documentNumber;

    @Column(name = "correo", unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Person person;

    @Column(name = "activo", nullable = false)
    private Boolean active;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserRole> userRoles;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }
}
