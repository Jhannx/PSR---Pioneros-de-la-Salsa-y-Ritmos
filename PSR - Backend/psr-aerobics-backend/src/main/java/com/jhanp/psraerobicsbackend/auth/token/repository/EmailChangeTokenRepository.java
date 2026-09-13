package com.jhanp.psraerobicsbackend.auth.token.repository;

import com.jhanp.psraerobicsbackend.auth.token.entity.EmailChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Integer> {

    /**
     * El canje. Se busca por hash porque es lo único que hay guardado: llega el token en claro, se
     * hashea y se compara. Trae la cuenta en la misma consulta, que es a la que hay que escribirle el
     * correo nuevo justo después.
     */
    @Query("""
            SELECT t FROM EmailChangeToken t
            JOIN FETCH t.user
            WHERE t.tokenHash = :tokenHash
            """)
    Optional<EmailChangeToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    /** Pedir un cambio nuevo invalida el anterior; además usuario_id es UNIQUE. */
    @Modifying
    @Query("DELETE FROM EmailChangeToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM EmailChangeToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
