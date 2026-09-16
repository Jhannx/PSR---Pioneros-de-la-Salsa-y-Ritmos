package com.jhanp.psraerobicsbackend.email;

import java.util.List;

/**
 * Transporte de correo, sin ninguna noción del dominio: recibe un destinatario y un HTML ya armado.
 * Quién arma ese HTML —invitación, recuperación de contraseña— es asunto de la capa de arriba.
 * <p>
 * Hay dos implementaciones y se elige por configuración con app.email.provider: {@code smtp} para
 * desarrollo y {@code brevo} para producción. El resto del código depende solo de esta interfaz, así
 * que cambiar de proveedor no toca ni una línea fuera de este paquete.
 */
public interface EmailSender {

    /**
     * Envía de forma síncrona. Lanza {@link com.jhanp.psraerobicsbackend.shared.exception.EmailException}
     * si el envío falla.
     * <p>
     * Se deja síncrono a propósito, para que quien llama decida qué hacer con el fallo: en el alta de
     * usuario el correo es un extra —la cuenta ya existe y se puede entrar con el documento— así que
     * ahí conviene capturar y seguir, no tumbar la transacción.
     *
     * @param toAddress destinatario; nunca null ni vacío
     * @param toName    nombre visible del destinatario, opcional (puede ir null)
     * @param subject   asunto en texto plano
     * @param htmlBody  cuerpo en HTML
     */
    default void send(String toAddress, String toName, String subject, String htmlBody) {
        send(toAddress, toName, subject, htmlBody, List.of());
    }

    /**
     * Igual que {@link #send(String, String, String, String)} pero con archivos adjuntos.
     * <p>
     * Es el método que implementan los proveedores; el de cuatro argumentos delega aquí con la lista
     * vacía. Así los tres correos que no llevan adjunto siguen escribiéndose igual que siempre y no
     * hubo que tocarlos.
     *
     * @param attachments nunca null; lista vacía si no hay adjuntos
     */
    void send(String toAddress, String toName, String subject, String htmlBody,
              List<EmailAttachment> attachments);
}
