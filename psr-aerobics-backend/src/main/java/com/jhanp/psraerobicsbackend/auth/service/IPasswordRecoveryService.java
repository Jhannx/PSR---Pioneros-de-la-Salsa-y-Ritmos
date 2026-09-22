package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ForgotPasswordRequest;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;

public interface IPasswordRecoveryService {

    /**
     * "Olvidé mi contraseña". Responde el mismo mensaje exista o no el correo, para no filtrar qué
     * direcciones están registradas.
     */
    MessageResponse requestReset(ForgotPasswordRequest request);
}
