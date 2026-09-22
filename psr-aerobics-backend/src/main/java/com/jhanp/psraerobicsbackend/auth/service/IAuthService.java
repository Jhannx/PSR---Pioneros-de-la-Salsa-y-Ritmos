package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ChangePasswordRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.LoginRequest;
import com.jhanp.psraerobicsbackend.auth.dto.response.AuthResponse;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;

/**
 * Entrar y cambiar la contraseña conociéndola. El alta no está aquí: vive en {@code RegisterService},
 * detrás de /api/personas, porque en PSR toda cuenta nace pegada a una persona.
 */
public interface IAuthService {

    AuthResponse login(LoginRequest request);

    /** Cambio de contraseña estando ya autenticado, conociendo la actual. */
    MessageResponse changePassword(String identifier, ChangePasswordRequest request);
}
