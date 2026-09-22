package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.event.AuthEmailEvents;
import com.jhanp.psraerobicsbackend.auth.token.enums.TokenType;
import com.jhanp.psraerobicsbackend.auth.token.service.PasswordTokenService;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * POLÍTICA del caso 1: cuenta recién creada, desde admin o por autoregistro.
 * <p>
 * Ventana larga (48 h por defecto) porque la persona no está esperando el correo: puede abrirlo días
 * después. Y aunque el enlace caduque no se queda fuera, porque su número de documento sigue
 * sirviendo como contraseña y siempre puede pedir uno nuevo desde «Olvidé mi contraseña».
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService implements IInvitationService {

    private final PasswordTokenService tokenService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.password-token.invitation-expiration-hours}")
    private long expirationHours;

    @Override
    @Transactional
    public void invite(Integer userId, boolean passwordIsDocument) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la cuenta " + userId));

        // Sin correo no hay a quién escribir, y emitir un token que nadie va a recibir solo ocuparía
        // la fila UNIQUE del usuario. Es el caso normal de un alta en el kiosko.
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.debug("La cuenta {} no tiene correo; no se envía invitación", userId);
            return;
        }

        PasswordTokenService.GeneratedToken generated =
                tokenService.create(user, TokenType.INVITATION, Duration.ofHours(expirationHours));

        // Los datos del correo se extraen AQUÍ, dentro de la transacción: getPerson() es perezoso y el
        // listener corre con la sesión ya cerrada.
        eventPublisher.publishEvent(new AuthEmailEvents.Welcome(
                user.getEmail(),
                PersonNames.fullName(user.getPerson()),
                user.getDocumentNumber(),
                generated.plainValue(),
                expirationHours,
                passwordIsDocument));

        log.info("Invitación emitida para el usuario ID {}", userId);
    }
}
