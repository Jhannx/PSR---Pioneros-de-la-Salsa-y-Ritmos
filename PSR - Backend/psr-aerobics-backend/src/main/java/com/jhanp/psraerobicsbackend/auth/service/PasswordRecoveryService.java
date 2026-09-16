package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ForgotPasswordRequest;
import com.jhanp.psraerobicsbackend.auth.event.AuthEmailEvents;
import com.jhanp.psraerobicsbackend.auth.token.enums.TokenType;
import com.jhanp.psraerobicsbackend.auth.token.service.PasswordTokenService;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * POLÍTICA del caso 2: "olvidé mi contraseña".
 * <p>
 * Ventana corta (30 min por defecto) porque la persona está delante de la pantalla esperando el
 * correo: cuanto menos dure el enlace, menos ventana hay para que alguien con acceso a ese buzón lo
 * aproveche.
 * <p>
 * <b>Este endpoint revela si un correo está registrado, y es a propósito.</b> Antes respondía lo mismo
 * existiera o no, para no servir de oráculo de cuentas. Se cambió el 2026-09-12 por decisión de
 * producto: el público objetivo son personas mayores de edad en un entorno cerrado, y ahí una UX clara
 * pesa más que la seguridad estricta — el mensaje ambiguo dejaba a la gente sin saber si se había
 * equivocado de correo o si el sistema estaba fallando.
 * <p>
 * El coste que se acepta a cambio: cualquiera puede ir probando direcciones contra este endpoint y
 * quedarse con las que existen. Con el tamaño y el público de la academia se consideró asumible.
 * <p>
 * <b>No es un descuido: si una auditoría marca esto, que se consulte antes de "arreglarlo".</b> La
 * decisión equivalente para el 401 de login bloqueado NO se tomó — ese sigue siendo deliberadamente
 * genérico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryService implements IPasswordRecoveryService {

    private final PasswordTokenService tokenService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.password-token.recovery-expiration-minutes}")
    private long expirationMinutes;

    @Override
    @Transactional
    public MessageResponse requestReset(ForgotPasswordRequest request) {
        log.info("Solicitud de recuperación de contraseña recibida");

        String email = request.email().trim().toLowerCase();

        // Correo desconocido: se dice. Ver la nota de política en el javadoc de la clase — el 404 es
        // intencionado y no debe volver a unificarse con el caso de éxito sin revisar esa decisión.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Recuperación solicitada para un correo no registrado");
                    return new ResourceNotFoundException(
                            "El correo ingresado no existe en nuestra base de datos.");
                });

        PasswordTokenService.GeneratedToken generated = tokenService.create(
                user, TokenType.RECOVERY, Duration.ofMinutes(expirationMinutes));

        eventPublisher.publishEvent(new AuthEmailEvents.PasswordRecovery(
                user.getEmail(),
                PersonNames.fullName(user.getPerson()),
                generated.plainValue(),
                expirationMinutes));

        return new MessageResponse("Se envió un enlace de recuperación a tu correo.");
    }
}
