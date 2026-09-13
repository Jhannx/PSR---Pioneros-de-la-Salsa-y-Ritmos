package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ResetPasswordRequest;
import com.jhanp.psraerobicsbackend.auth.event.AuthEmailEvents;
import com.jhanp.psraerobicsbackend.auth.token.service.PasswordTokenService;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El canje. Es donde los dos flujos vuelven a juntarse: {@code tokenService.consume()} valida y quema
 * el token sin importar de dónde vino, y aquí solo se escribe la contraseña.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService implements IPasswordResetService {

    private final PasswordTokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MessageResponse reset(ResetPasswordRequest request) {
        // Si vuelve sin excepción, el token era válido y ya está quemado.
        PasswordTokenService.ConsumedToken consumed = tokenService.consume(request.token());

        User user = consumed.user();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Aviso al dueño de la cuenta: si no fue él, es su señal de que algo pasa.
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            eventPublisher.publishEvent(new AuthEmailEvents.PasswordChanged(
                    user.getEmail(),
                    PersonNames.fullName(user.getPerson())));
        }

        log.info("Contraseña establecida ({}) para el usuario ID {}", consumed.type(), user.getId());
        return new MessageResponse("Contraseña actualizada correctamente.");
    }
}
