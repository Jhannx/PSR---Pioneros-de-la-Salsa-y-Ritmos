package com.jhanp.psraerobicsbackend.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /api/personas/{id}/solicitar-cambio-correo.
 * <p>
 * Solo el correo nuevo: la cuenta sale del id de la ruta, y no se pide la contraseña actual porque
 * quien llama o es el dueño autenticado o es el mostrador actuando en su nombre. La verificación de
 * verdad la hace el enlace que llega al buzón nuevo.
 */
public record EmailChangeRequest(
        @NotBlank(message = "El correo nuevo es obligatorio")
        @Email(message = "El formato del correo no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String newEmail
) {}
