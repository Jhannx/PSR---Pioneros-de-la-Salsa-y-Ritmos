package com.jhanp.psraerobicsbackend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Entrada de POST /api/auth/forgot-password. Solo correo: una cuenta sin correo registrado no puede
 * recuperar por sí sola, la reestablece un admin.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato del correo no es válido")
        String email
) {}
