package com.jhanp.psraerobicsbackend.email.service;

import com.jhanp.psraerobicsbackend.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Decide QUÉ correo se manda: qué plantilla, qué variables, qué asunto.
 * <p>
 * No sabe entregar nada. Eso es trabajo del {@link EmailSender} que le inyecten, que será el de SMTP o
 * el de la API de Brevo según app.email.provider. Gracias a esa separación, añadir un tercer proveedor
 * mañana no toca ni una línea de esta clase ni de las plantillas.
 * <p>
 * El flujo de cada correo: se arma un Context de Thymeleaf con las variables que la plantilla
 * necesita, el motor convierte plantilla + variables en un String de HTML, y ese HTML se le pasa al
 * EmailSender, que lo entrega como pueda.
 */
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private static final Locale EMAIL_LOCALE = Locale.of("es", "CO");

    /** Pantalla del front donde la persona escribe su contraseña nueva. Lee el token de la query. */
    private static final String RESET_PATH = "/restablecer-password";

    private static final String LOGIN_PATH = "/login";

    /** Pantalla del front que canjea el token de cambio de correo. Análoga a la de restablecer. */
    private static final String EMAIL_CHANGE_PATH = "/confirmar-correo";

    private final ITemplateEngine templateEngine;
    private final EmailSender emailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void sendWelcome(String toAddress, String toName, String documentNumber,
                            String token, long validityHours, boolean passwordIsDocument) {
        // HashMap y no Map.of: toName puede ser null si la cuenta no tiene persona asociada, y Map.of
        // no admite nulos.
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", toName);
        variables.put("correo", toAddress);
        variables.put("documento", documentNumber);
        variables.put("enlaceLogin", buildLink(LOGIN_PATH, null));
        variables.put("enlaceCambio", buildLink(RESET_PATH, token));
        variables.put("horas", validityHours);
        // Condiciona las dos frases de la plantilla que afirman que la contraseña es el documento.
        variables.put("passwordEsDocumento", passwordIsDocument);

        send(toAddress, toName, "Tu perfil en PSR Aeróbicos está listo", "email/bienvenida", variables);
    }

    @Override
    public void sendPasswordRecovery(String toAddress, String toName, String token, long validityMinutes) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", toName);
        variables.put("enlace", buildLink(RESET_PATH, token));
        variables.put("minutos", validityMinutes);

        send(toAddress, toName, "Restablece tu contraseña", "email/recuperar-password", variables);
    }

    @Override
    public void sendEmailChangeConfirmation(String toAddress, String toName, String token, long validityHours) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", toName);
        variables.put("correoNuevo", toAddress);
        variables.put("enlace", buildLink(EMAIL_CHANGE_PATH, token));
        variables.put("horas", validityHours);

        send(toAddress, toName, "Confirma tu correo nuevo", "email/cambio-correo", variables);
    }

    @Override
    public void sendPasswordChanged(String toAddress, String toName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombre", toName);

        send(toAddress, toName, "Tu contraseña fue actualizada", "email/password-actualizada", variables);
    }

    /**
     * El enlace apunta al FRONT, no al backend: la persona necesita una pantalla donde escribir su
     * contraseña nueva. Esa pantalla lee el token de la query string y lo manda a
     * POST /api/auth/reset-password.
     */
    private String buildLink(String path, String token) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendUrl).path(path);
        if (token != null) {
            builder.queryParam("token", token);
        }
        return builder.build().toUriString();
    }

    private void send(String toAddress, String toName, String subject,
                      String template, Map<String, Object> variables) {
        String htmlBody = templateEngine.process(template, new Context(EMAIL_LOCALE, variables));
        emailSender.send(toAddress, toName, subject, htmlBody);
    }
}
