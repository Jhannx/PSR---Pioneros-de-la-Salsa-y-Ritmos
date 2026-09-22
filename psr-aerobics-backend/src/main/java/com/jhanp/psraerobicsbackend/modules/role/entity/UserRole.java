package com.jhanp.psraerobicsbackend.modules.role.entity;

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
@Table(name = "usuario_rol")
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", nullable = false)
    private Role role;

    @Column(name = "asignado_en", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        this.assignedAt = OffsetDateTime.now();
    }
}
