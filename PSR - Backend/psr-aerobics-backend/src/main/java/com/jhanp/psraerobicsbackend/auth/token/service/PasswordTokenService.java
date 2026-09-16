package com.jhanp.psraerobicsbackend.auth.token.service;

import com.jhanp.psraerobicsbackend.auth.token.TokenSecrets;
import com.jhanp.psraerobicsbackend.auth.token.entity.PasswordToken;
import com.jhanp.psraerobicsbackend.auth.token.enums.TokenType;
import com.jhanp.psraerobicsbackend.auth.token.repository.PasswordTokenRepository;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * La MECÁNICA de los tokens: generar, hashear, validar, quemar y limpiar.
 * <p>
 * No sabe por qué se emite un token ni qué correo sale después. Eso es la POLÍTICA, y vive en un
 * servicio por caso de uso: {@code InvitationService} y {@code PasswordRecoveryService}. Esta clase
 * existe para que esos dos no dupliquen la mecánica de emitir y quemar.
 * <p>
 * La parte criptográfica —fabricar el token y hashearlo— está un escalón más abajo, en
 * {@link com.jhanp.psraerobicsbackend.auth.token.TokenSecrets}, porque la comparte con el cambio de
 * correo, que usa su propia tabla.
 * <p>
 * Es la única clase de servicio de auth sin interfaz, y a propósito: una interfaz sirve para
 * desacoplar a quien te consume, y aquí los consumidores son sus vecinos del mismo módulo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordTokenService {

    private final PasswordTokenRepository tokenRepository;

    /**
     * La fila guardada y el token en claro. Existen juntos solo aquí: la fila va a la base de datos
     * con el hash, el claro va al correo, y nadie más los ve a la vez.
     */
    public record GeneratedToken(PasswordToken entity, String plainValue) {}

    /** Lo que queda tras quemar un token válido: a quién pertenecía y por qué se emitió. */
    public record ConsumedToken(User user, TokenType type) {}

    /**
     * Emite un token para el usuario. Devuelve el valor EN CLARO: es la única vez que existe fuera del
     * buzón de la persona, así que quien lo reciba debe meterlo en un correo y olvidarlo, nunca
     * guardarlo ni loguearlo.
     */
    @Transactional
    public GeneratedToken create(User user, TokenType type, Duration duration) {
        // Pedir un token nuevo invalida el anterior. Además usuario_id es UNIQUE, así que sin este
        // borrado el insert fallaría en la segunda solicitud.
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        String plainToken = TokenSecrets.newToken();

        PasswordToken saved = tokenRepository.save(PasswordToken.builder()
                .user(user)
                .tokenHash(TokenSecrets.hash(plainToken))
                .type(type)
                .expiresAt(OffsetDateTime.now().plus(duration))
                .build());

        log.info("Token {} emitido para el usuario ID {}", type, user.getId());
        return new GeneratedToken(saved, plainToken);
    }

    /**
     * Valida el token recibido y lo quema en el mismo paso. Si vuelve sin excepción, quien llama tiene
     * permiso para cambiarle la contraseña a ese usuario.
     * <p>
     * Devuelve un record y no la entidad justamente porque la entidad ya está marcada para borrado:
     * entregar un objeto muerto invita a que alguien lo siga usando.
     */
    @Transactional
    public ConsumedToken consume(String plainToken) {
        // Se hashea lo que llega y se busca por hash: si el token es válido, coincidirá.
        PasswordToken token = tokenRepository.findByTokenHash(TokenSecrets.hash(plainToken))
                .orElseThrow(() -> {
                    log.warn("Intento de canjear un token inexistente");
                    return new InvalidDataException("El enlace no es válido o ya fue utilizado");
                });

        User user = token.getUser();
        TokenType type = token.getType();

        if (token.isExpired()) {
            log.warn("Token {} expirado para el usuario ID {}", type, user.getId());
            tokenRepository.delete(token);
            throw new InvalidDataException(
                    "El enlace expiró. Solicita uno nuevo desde \"Olvidé mi contraseña\".");
        }

        // Un solo uso: si alguien reenvía el mismo enlace, ya no sirve.
        tokenRepository.delete(token);

        return new ConsumedToken(user, type);
    }

    /**
     * Los tokens caducados ya no sirven para nada, pero siguen ocupando la fila UNIQUE del usuario y
     * engordando la tabla. Se barren de madrugada.
     */
    @Scheduled(cron = "${app.password-token.cleanup-cron}")
    @Transactional
    public int cleanUpExpired() {
        int deleted = tokenRepository.deleteExpired(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Limpieza programada: {} token(s) caducado(s) eliminado(s)", deleted);
        }
        return deleted;
    }

}
