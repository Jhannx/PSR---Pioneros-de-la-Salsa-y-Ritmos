package com.jhanp.psraerobicsbackend.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Entrada de POST /api/auth/login.
 * <p>
 * El identificador es número de documento <b>o</b> correo: la tabla usuario acepta los dos y exige al
 * menos uno, así que quien inicia sesión escribe el que tenga. Por eso no lleva {@code @Email}: esa
 * anotación dejaría fuera a toda cuenta creada sin correo, que hoy son casi todas.
 */
public record LoginRequest(
        @NotBlank(message = "El número de documento o correo es obligatorio")
        String identifier,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
