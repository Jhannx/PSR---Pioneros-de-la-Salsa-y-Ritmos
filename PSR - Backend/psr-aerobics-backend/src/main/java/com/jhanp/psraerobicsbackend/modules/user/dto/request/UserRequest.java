package com.jhanp.psraerobicsbackend.modules.user.dto.request;

import jakarta.validation.constraints.*;

public record UserRequest(
        @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres")
        String documentNumber,

        @Email(message = "El correo debe tener un formato válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password,

        @Positive(message = "El ID de persona debe ser positivo")
        Integer personId
) {
}
