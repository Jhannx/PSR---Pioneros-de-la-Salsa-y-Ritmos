package com.jhanp.psraerobicsbackend.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jhanp.psraerobicsbackend.shared.exception.EmailException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;

/**
 * Envío por la API HTTP de Brevo. Es el proveedor de producción: no depende del puerto 25/587, que
 * muchos hostings bloquean, y devuelve el error real en el cuerpo de la respuesta en vez de un
 * timeout opaco.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
@Slf4j
public class BrevoApiEmailSender implements EmailSender {

    private final EmailProperties properties;
    private final RestClient restClient;

    public BrevoApiEmailSender(EmailProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.brevo().url())
                .defaultHeader("api-key", properties.brevo().apiKey())
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Sin API key no hay envío posible, y con este proveedor activo eso es un error de despliegue.
     * Mejor reventar al arrancar que descubrirlo cuando el primer usuario se quede sin su correo.
     */
    @PostConstruct
    void validateApiKey() {
        if (properties.brevo().apiKey().isBlank()) {
            throw new IllegalStateException(
                    "app.email.provider=brevo pero BREVO_API_KEY está vacío");
        }
    }

    @Override
    public void send(String toAddress, String toName, String subject, String htmlBody,
                     List<EmailAttachment> attachments) {

        // null y no lista vacía cuando no hay adjuntos: Brevo rechaza "attachment": [] con un 400.
        // Jackson omite el campo si es null, que es justo lo que hace falta.
        List<BrevoAttachment> encoded = attachments.isEmpty() ? null : attachments.stream()
                .map(attachment -> new BrevoAttachment(
                        attachment.filename(),
                        Base64.getEncoder().encodeToString(attachment.content())))
                .toList();

        BrevoRequest body = new BrevoRequest(
                new Contact(properties.fromAddress(), properties.fromName()),
                List.of(new Contact(toAddress, toName)),
                subject,
                htmlBody,
                encoded);

        try {
            restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Correo enviado por Brevo a {} con asunto '{}'{}", toAddress, subject,
                    encoded != null ? " y " + encoded.size() + " adjunto(s)" : "");

        } catch (RestClientException e) {
            // Cubre tanto los 4xx/5xx que lanza retrieve() como los fallos de red.
            throw new EmailException("Brevo rechazó el envío del correo a " + toAddress, e);
        }
    }

    /**
     * Forma exacta que espera POST /v3/smtp/email. Se declaran aquí como records anidados porque no
     * se usan en ningún otro sitio: son el contrato con Brevo, no DTOs de la aplicación.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record BrevoRequest(Contact sender, List<Contact> to, String subject, String htmlContent,
                                List<BrevoAttachment> attachment) {}

    private record Contact(String email, String name) {}

    /**
     * El campo se llama {@code attachment} en singular aunque sea una lista, y el contenido va en
     * base64 dentro del propio JSON: así lo define la API de Brevo. No es un descuido de nombres.
     */
    private record BrevoAttachment(String name, String content) {}
}
