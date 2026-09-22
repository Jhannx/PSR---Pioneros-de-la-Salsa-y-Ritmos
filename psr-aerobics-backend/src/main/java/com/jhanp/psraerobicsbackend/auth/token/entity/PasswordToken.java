package com.jhanp.psraerobicsbackend.auth.token.entity;

import com.jhanp.psraerobicsbackend.auth.token.enums.TokenType;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Token de un solo uso, con caducidad, que autoriza a fijar una contraseña sin conocer la anterior.
 * El campo tipo dice por qué se emitió; el mecanismo es idéntico en ambos casos.
 * <p>
 * La tabla sigue llamándose token_recuperacion porque el esquema no se toca, pero cubre también las
 * invitaciones de alta.
 */
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "token_recuperacion")
public class PasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User user;

    /**
     * Hash SHA-256 del token, NO el token. El valor original solo existe dentro del correo: ni la base
     * de datos ni los logs lo ven nunca. Al recibir uno se hashea y se busca por este campo.
     */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    // Sin @Enumerated: TokenTypeConverter se aplica solo y guarda el valor en español.
    @Column(name = "tipo", nullable = false, length = 20)
    private TokenType type;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "fecha_expiracion", nullable = false)
    private OffsetDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(OffsetDateTime.now());
    }
}
