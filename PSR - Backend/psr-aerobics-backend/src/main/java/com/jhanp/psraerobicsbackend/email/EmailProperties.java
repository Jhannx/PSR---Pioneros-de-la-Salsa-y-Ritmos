package com.jhanp.psraerobicsbackend.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del envío de correo (prefijo app.email en application.properties).
 * <p>
 * El remitente vive aquí y no en cada implementación porque es el mismo para las dos: cambiar la
 * dirección de salida no debería obligar a tocar código.
 */
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(

        /** smtp o brevo. Decide cuál de los dos EmailSender se registra como bean. */
        @DefaultValue("smtp") String provider,

        /** Dirección remitente. En Brevo tiene que ser un remitente verificado en la cuenta. */
        String fromAddress,

        @DefaultValue("PSR Aerobicos") String fromName,

        @DefaultValue Brevo brevo
) {

    public record Brevo(
            @DefaultValue("") String apiKey,
            @DefaultValue("https://api.brevo.com/v3/smtp/email") String url
    ) {}
}
