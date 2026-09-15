package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.EmailChangeConfirmRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.EmailChangeRequest;
import com.jhanp.psraerobicsbackend.auth.event.AuthEmailEvents;
import com.jhanp.psraerobicsbackend.auth.token.TokenSecrets;
import com.jhanp.psraerobicsbackend.auth.token.entity.EmailChangeToken;
import com.jhanp.psraerobicsbackend.auth.token.repository.EmailChangeTokenRepository;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.DuplicateResourceException;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Los dos pasos del cambio de correo. Tiene su propia mecánica de tokens en vez de reutilizar
 * {@code PasswordTokenService} porque necesita guardar el correo destino, y la tabla de aquel no
 * tiene dónde; lo que sí comparten es {@link TokenSecrets}, que es la parte que no debe divergir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailChangeService implements IEmailChangeService {

    private final EmailChangeTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.email-change.expiration-hours}")
    private long expirationHours;

    @Override
    @Transactional
    public MessageResponse request(Integer personId, EmailChangeRequest request) {
        User user = userRepository.findByPersonId(personId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La persona " + personId + " no tiene cuenta de acceso"));

        String newEmail = normalizeEmail(request.newEmail());

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new InvalidDataException("Ese ya es el correo de la cuenta");
        }

        // Se comprueba aquí para responder 409 con un mensaje claro, y se vuelve a comprobar al
        // confirmar: entre las dos cosas pueden pasar días, y en ese rato alguien más puede haberse
        // registrado con ese correo.
        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateResourceException("Ya existe una cuenta con el correo " + newEmail);
        }

        // Pedir un cambio nuevo invalida el anterior. Además usuario_id es UNIQUE, así que sin este
        // borrado el insert fallaría en la segunda solicitud.
        tokenRepository.deleteByUserId(user.getId());
        tokenRepository.flush();

        String plainToken = TokenSecrets.newToken();

        tokenRepository.save(EmailChangeToken.builder()
                .user(user)
                .newEmail(newEmail)
                .tokenHash(TokenSecrets.hash(plainToken))
                .expiresAt(OffsetDateTime.now().plus(Duration.ofHours(expirationHours)))
                .build());

        // Al correo NUEVO, nunca al viejo: el enlace es justamente la prueba de que quien pide el
        // cambio tiene acceso a ese buzón.
        eventPublisher.publishEvent(new AuthEmailEvents.EmailChangeRequested(
                newEmail,
                PersonNames.fullName(user.getPerson()),
                plainToken,
                expirationHours));

        log.info("Cambio de correo solicitado para la cuenta ID {}", user.getId());
        return new MessageResponse(
                "Te enviamos un enlace de confirmación al correo nuevo. El cambio se aplica cuando lo abras.");
    }

    @Override
    @Transactional
    public MessageResponse confirm(EmailChangeConfirmRequest request) {
        // Se hashea lo que llega y se busca por hash: si el token es válido, coincidirá.
        EmailChangeToken token = tokenRepository.findByTokenHash(TokenSecrets.hash(request.token()))
                .orElseThrow(() -> {
                    log.warn("Intento de canjear un token de cambio de correo inexistente");
                    return new InvalidDataException("El enlace no es válido o ya fue utilizado");
                });

        User user = token.getUser();
        String newEmail = token.getNewEmail();

        // Se consume ANTES de validar nada más, y ese orden importa.
        //
        // Consumirlo es lo que convierte "un solo uso" en una garantía: quien se lleve la fila es el
        // único que sigue, y cualquier otra llegada —doble clic, dos pestañas, un reintento de red—
        // se encuentra con 0 y sale por el mensaje de abajo. Si en vez de esto se comprobara primero
        // y se borrara al final, dos peticiones simultáneas pasarían las dos comprobaciones y la
        // segunda reventaría con "Unexpected row count", que es un 500 sin explicación.
        //
        // Que el token se gaste aunque después falle por expirado o por correo duplicado es correcto
        // y ya era el comportamiento anterior: en los dos casos ese enlace no vuelve a servir.
        if (tokenRepository.consume(token.getId()) == 0) {
            log.warn("Token de cambio de correo ya consumido para la cuenta ID {}", user.getId());
            throw new InvalidDataException("El enlace no es válido o ya fue utilizado");
        }

        if (token.isExpired()) {
            log.warn("Token de cambio de correo expirado para la cuenta ID {}", user.getId());
            throw new InvalidDataException("El enlace expiró. Solicita el cambio de correo otra vez.");
        }

        // Segunda comprobación, la que de verdad protege: entre la solicitud y el canje puede haber
        // pasado cualquier cosa. Sin esto, el UNIQUE de usuario.correo saltaría como error genérico
        // de base de datos y la persona vería un 500 en vez de saber qué pasó.
        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new DuplicateResourceException(
                    "Ya existe una cuenta con el correo " + newEmail + ". El cambio no se aplicó.");
        }

        user.setEmail(newEmail);
        userRepository.save(user);

        log.info("Correo actualizado para la cuenta ID {}", user.getId());
        return new MessageResponse("Tu correo quedó actualizado. Úsalo la próxima vez que entres.");
    }

    /**
     * Los tokens caducados ya no sirven, pero siguen ocupando la fila UNIQUE del usuario: mientras uno
     * siga ahí, nada impediría pedir otro cambio (la solicitud borra el anterior), pero la tabla
     * engorda sin motivo. Se barren con el mismo cron que los de contraseña.
     */
    @Scheduled(cron = "${app.password-token.cleanup-cron}")
    @Transactional
    public int cleanUpExpired() {
        int deleted = tokenRepository.deleteExpired(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Limpieza programada: {} token(s) de cambio de correo caducado(s) eliminado(s)", deleted);
        }
        return deleted;
    }

    /**
     * Minúsculas y sin espacios, como en UserService: el UNIQUE de la base ignora mayúsculas
     * (uq_usuario_correo_lower), así que guardarlo de otra forma solo serviría para que la persona
     * viera su correo escrito distinto a como lo escribe.
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
