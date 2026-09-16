package com.jhanp.psraerobicsbackend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Entrada de POST /api/personas/confirmar-cambio-correo. El token es la única credencial: dice a qué
 * cuenta y a qué correo se aplica el cambio, así que la pantalla que lo canjea no necesita sesión.
 */
public record EmailChangeConfirmRequest(
        @NotBlank(message = "El token es obligatorio")
        String token
) {}
