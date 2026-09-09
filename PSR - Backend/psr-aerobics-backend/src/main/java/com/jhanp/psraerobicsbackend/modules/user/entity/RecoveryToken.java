package com.jhanp.psraerobicsbackend.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "token_recuperacion")
public class RecoveryToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "usado", nullable = false)
    private Boolean used;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.used == null) {
            this.used = false;
        }
    }
}
