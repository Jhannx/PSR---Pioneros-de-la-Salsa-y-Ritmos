package com.jhanp.psraerobicsbackend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /api/auth/reset-password. El mismo DTO sirve para el token de invitación (alta
 * desde admin) y para el de olvido: los dos son filas de token_recuperacion y se canjean igual.
 */
public record ResetPasswordRequest(
        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String newPassword
) {}
