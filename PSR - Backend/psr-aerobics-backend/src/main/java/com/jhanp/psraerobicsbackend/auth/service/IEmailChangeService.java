package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.EmailChangeConfirmRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.EmailChangeRequest;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;

/**
 * Cambiar el correo de una cuenta, en dos pasos.
 * <p>
 * En dos y no en uno porque el correo es la llave de la recuperación de contraseña: quien pudiera
 * cambiarlo de un solo golpe podría apuntar una cuenta ajena a un buzón propio y entrar por
 * "olvidé mi contraseña". Con la confirmación de por medio, el cambio solo lo completa quien
 * demuestre tener acceso al buzón nuevo.
 */
public interface IEmailChangeService {

    /**
     * Emite el enlace y lo manda al correo NUEVO. No toca la cuenta: hasta la confirmación, la
     * persona sigue entrando con el correo de siempre y puede darse cuenta de que hay un cambio
     * pendiente que no pidió.
     *
     * @param personId id de PERSONA, el que lleva la ruta; la cuenta se resuelve a partir de él
     */
    MessageResponse request(Integer personId, EmailChangeRequest request);

    /** Canjea el enlace y escribe el correo nuevo en la cuenta. El token es de un solo uso. */
    MessageResponse confirm(EmailChangeConfirmRequest request);
}
