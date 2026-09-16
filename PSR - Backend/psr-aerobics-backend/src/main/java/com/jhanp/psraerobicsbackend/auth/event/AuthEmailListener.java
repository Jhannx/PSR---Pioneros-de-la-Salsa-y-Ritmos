package com.jhanp.psraerobicsbackend.auth.event;

import com.jhanp.psraerobicsbackend.email.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Único punto donde auth habla con el módulo de correo. Los servicios publican un evento y se olvidan.
 * <p>
 * Dos razones para AFTER_COMMIT en vez de un @EventListener normal:
 * <ul>
 *   <li>El token tiene que estar confirmado antes de que salga el enlace. Si se enviara dentro de la
 *       transacción y esta hiciera rollback, la persona recibiría un enlace muerto.</li>
 *   <li>Un fallo de correo no puede tumbar el alta. La cuenta ya existe y se puede entrar con el
 *       número de documento; que el SMTP esté caído es un problema aparte, y aquí solo se registra.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEmailListener {

    private final IEmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWelcome(AuthEmailEvents.Welcome event) {
        send("bienvenida", event.toAddress(), () -> emailService.sendWelcome(
                event.toAddress(), event.toName(), event.documentNumber(),
                event.token(), event.validityHours(), event.passwordIsDocument()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordRecovery(AuthEmailEvents.PasswordRecovery event) {
        send("recuperación", event.toAddress(), () -> emailService.sendPasswordRecovery(
                event.toAddress(), event.toName(), event.token(), event.validityMinutes()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordChanged(AuthEmailEvents.PasswordChanged event) {
        send("aviso de cambio", event.toAddress(), () -> emailService.sendPasswordChanged(
                event.toAddress(), event.toName()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailChangeRequested(AuthEmailEvents.EmailChangeRequested event) {
        send("confirmación de cambio de correo", event.toAddress(), () -> emailService.sendEmailChangeConfirmation(
                event.toAddress(), event.toName(), event.token(), event.validityHours()));
    }

    /**
     * El destinatario se registra, el motivo del fallo también, pero nunca el token: es la credencial
     * que viaja en el enlace y no debe quedar en los logs.
     */
    private void send(String kind, String toAddress, Runnable action) {
        try {
            action.run();
            log.info("Correo de {} enviado a {}", kind, toAddress);

        } catch (Exception e) {
            log.error("No se pudo enviar el correo de {} a {}: {}", kind, toAddress, e.getMessage());
        }
    }
}
