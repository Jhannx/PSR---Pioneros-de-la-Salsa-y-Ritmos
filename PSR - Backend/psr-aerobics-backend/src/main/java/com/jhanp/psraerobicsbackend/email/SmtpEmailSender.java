package com.jhanp.psraerobicsbackend.email;

import com.jhanp.psraerobicsbackend.shared.exception.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Envío por SMTP corriente, con las credenciales de spring.mail.*. Es el proveedor de desarrollo:
 * apunta a un Mailtrap o a un relay y no necesita más que usuario y contraseña.
 * <p>
 * matchIfMissing = true para que el arranque no se quede sin ningún EmailSender si nadie definió
 * app.email.provider: sin eso, la app fallaría al inyectar la dependencia en vez de avisar aquí.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, EmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(String toAddress, String toName, String subject, String htmlBody,
                     List<EmailAttachment> attachments) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // multipart solo cuando hace falta: un mensaje multipart sin partes que adjuntar añade
            // cabeceras y estructura MIME que algunos clientes pintan como un adjunto vacío. El
            // charset explícito evita que las tildes y las eñes lleguen rotas.
            boolean multipart = !attachments.isEmpty();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());

            helper.setFrom(properties.fromAddress(), properties.fromName());
            if (toName != null && !toName.isBlank()) {
                helper.setTo(new jakarta.mail.internet.InternetAddress(
                        toAddress, toName, StandardCharsets.UTF_8.name()));
            } else {
                helper.setTo(toAddress);
            }
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            for (EmailAttachment attachment : attachments) {
                helper.addAttachment(attachment.filename(),
                        new ByteArrayResource(attachment.content()),
                        attachment.contentType());
            }

            mailSender.send(message);
            log.info("Correo enviado por SMTP a {} con asunto '{}'{}", toAddress, subject,
                    multipart ? " y " + attachments.size() + " adjunto(s)" : "");

        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new EmailException("No se pudo enviar el correo a " + toAddress, e);
        }
    }
}
