package com.jhanp.psraerobicsbackend.email.service;

/**
 * Los correos concretos del sistema. Decide QUÉ se manda; entregarlo es asunto de
 * {@link com.jhanp.psraerobicsbackend.email.EmailSender}.
 * <p>
 * Todo llega como datos sueltos y no como entidad a propósito: estos métodos se invocan después del
 * commit, con la sesión de Hibernate ya cerrada, y resolver ahí un {@code user.getPerson()} perezoso
 * reventaría con LazyInitializationException.
 */
public interface IEmailService {

    /**
     * El perfil quedó creado. Lleva las dos cosas: cómo entrar ya mismo —con correo o documento— y un
     * enlace para poner una contraseña propia.
     *
     * @param token              token de invitación en claro, para el enlace de cambio
     * @param validityHours      cuánto dura ese enlace
     * @param passwordIsDocument true si la contraseña inicial es el número de documento. Con false, el
     *                           correo no afirma cuál es la contraseña: la persona eligió una propia
     *                           al registrarse y decirle que es su documento sería falso.
     */
    void sendWelcome(String toAddress, String toName, String documentNumber, String token,
                     long validityHours, boolean passwordIsDocument);

    /** "Olvidé mi contraseña": enlace para restablecerla. */
    void sendPasswordRecovery(String toAddress, String toName, String token, long validityMinutes);

    /** Aviso de que la contraseña acaba de cambiar, para detectar accesos no autorizados. */
    void sendPasswordChanged(String toAddress, String toName);

    /**
     * Confirmación de cambio de correo. Va al correo NUEVO: abrir el enlace es lo que demuestra que
     * quien pidió el cambio tiene acceso a ese buzón, y hasta entonces la cuenta no se toca.
     *
     * @param token         token de cambio en claro, para el enlace
     * @param validityHours cuánto dura ese enlace
     */
    void sendEmailChangeConfirmation(String toAddress, String toName, String token, long validityHours);
}
