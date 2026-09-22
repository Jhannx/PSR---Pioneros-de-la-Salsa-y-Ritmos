package com.jhanp.psraerobicsbackend.auth.dto.response;

/**
 * Salida de POST /api/auth/login: el token y quién entró.
 * <p>
 * Se devuelve el usuario junto al token para que el front sepa de una sola llamada quién es, qué
 * puede hacer y a qué persona corresponde, sin una segunda vuelta al servidor antes de pintar la
 * primera pantalla.
 */
public record AuthResponse(
        String token,
        AuthenticatedUserResponse user
) {}
