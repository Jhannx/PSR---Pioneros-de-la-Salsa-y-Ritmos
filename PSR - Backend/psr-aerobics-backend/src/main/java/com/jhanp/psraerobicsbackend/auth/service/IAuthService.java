package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.auth.dto.request.ChangePasswordRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.LoginRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.RegistrationRequest;
import com.jhanp.psraerobicsbackend.auth.dto.response.AuthResponse;
import com.jhanp.psraerobicsbackend.common.dto.MessageResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;

public interface IAuthService {

    AuthResponse login(LoginRequest request);

    /** Auto-registro público. Extra sobre el contrato; siempre crea un MIEMBRO. */
    UserResponse register(RegistrationRequest request);

    /** Cambio de contraseña estando ya autenticado, conociendo la actual. */
    MessageResponse changePassword(String identifier, ChangePasswordRequest request);
}
