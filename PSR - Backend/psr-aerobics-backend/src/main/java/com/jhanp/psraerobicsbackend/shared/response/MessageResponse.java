package com.jhanp.psraerobicsbackend.shared.response;

/**
 * Respuesta de las operaciones que solo confirman que algo salió bien y no devuelven un recurso:
 * recuperación de contraseña, restablecimiento, cambio de contraseña.
 */
public record MessageResponse(String message) {
}
