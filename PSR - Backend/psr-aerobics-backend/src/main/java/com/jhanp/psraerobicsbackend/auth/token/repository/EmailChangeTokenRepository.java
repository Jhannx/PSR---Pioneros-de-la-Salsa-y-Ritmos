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

    /**
     * Consume el token y dice si lo consiguió: 1 si esta llamada fue la que lo borró, 0 si otra se le
     * adelantó. Es la forma de que "un solo uso" lo decida la base y no una comprobación previa.
     * <p>
     * {@code delete(entidad)} no sirve para esto: Hibernate exige que la fila siga ahí y, si otra
     * transacción ya la borró, revienta con {@code Unexpected row count (expected 1 but was 0)} —una
     * excepción de infraestructura que sale como 500 sin mensaje. Pasa con un doble clic, dos pestañas
     * abiertas o un reintento de red, que en un enlace de correo no son casos raros.
     * <p>
     * flushAutomatically vacía antes lo que hubiera pendiente; no se limpia el contexto a propósito,
     * para que el User ya cargado siga gestionado y se le pueda escribir el correo nuevo después.
     */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM EmailChangeToken t WHERE t.id = :id")
    int consume(@Param("id") Integer id);

    @Modifying
    @Query("DELETE FROM EmailChangeToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
