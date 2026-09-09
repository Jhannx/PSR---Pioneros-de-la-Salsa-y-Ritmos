package com.jhanp.psraerobicsbackend.email;

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
    void send(String toAddress, String toName, String subject, String htmlBody);
}
