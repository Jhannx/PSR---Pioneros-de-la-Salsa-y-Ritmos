package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ResetPasswordRequest;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;

public interface IPasswordResetService {

    /**
     * Canjea el token del correo por una contraseña nueva. Sirve igual para el de invitación y para el
     * de olvido: los dos son filas de token_recuperacion y se queman igual.
     */
    MessageResponse reset(ResetPasswordRequest request);
}
