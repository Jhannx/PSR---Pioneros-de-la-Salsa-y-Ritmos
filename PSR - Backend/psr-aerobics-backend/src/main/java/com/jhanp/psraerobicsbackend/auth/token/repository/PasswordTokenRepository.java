package com.jhanp.psraerobicsbackend.auth.token.repository;

import com.jhanp.psraerobicsbackend.auth.token.entity.PasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface PasswordTokenRepository extends JpaRepository<PasswordToken, Integer> {

    /**
     * El canje. Se busca por hash porque es lo único que hay guardado: llega el token en claro, se
     * hashea y se compara. Trae el usuario en la misma consulta, que es a quien hay que cambiarle la
     * contraseña justo después.
     */
    @Query("""
            SELECT t FROM PasswordToken t
            JOIN FETCH t.user
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<PasswordToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    /** Pedir un token nuevo invalida el anterior; además usuario_id es UNIQUE. */
    @Modifying
    @Query("DELETE FROM PasswordToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM PasswordToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
