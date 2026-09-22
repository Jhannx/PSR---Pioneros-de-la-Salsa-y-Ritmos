package com.jhanp.psraerobicsbackend.auth.token.entity;

import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Un cambio de correo pedido y todavía sin confirmar: a qué cuenta, a qué dirección y con qué enlace.
 * <p>
 * Mientras esta fila exista, la cuenta sigue funcionando con el correo de siempre. El correo nuevo
 * solo se copia a {@code usuario.correo} al canjear el token, y eso solo puede hacerlo quien tenga
 * acceso al buzón nuevo.
 * <p>
 * Tabla propia y no {@code token_recuperacion}: aquella no tiene dónde guardar el correo destino y
 * lleva usuario_id UNIQUE, así que compartirla haría que pedir un cambio de correo invalidara una
 * recuperación de contraseña en curso.
 */
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Builder
@Table(name = "token_cambio_correo")
public class EmailChangeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User user;

    /** Ya normalizado a minúsculas y sin espacios, como el resto de correos del sistema. */
    @Column(name = "correo_nuevo", nullable = false, length = 150)
    private String newEmail;

    /**
     * Hash SHA-256 del token, NO el token. El valor original solo existe dentro del correo: ni la base
     * de datos ni los logs lo ven nunca. Al recibir uno se hashea y se busca por este campo.
     */
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

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
