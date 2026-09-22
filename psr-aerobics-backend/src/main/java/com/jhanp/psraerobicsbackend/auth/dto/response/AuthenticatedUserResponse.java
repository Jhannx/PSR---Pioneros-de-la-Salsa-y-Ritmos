package com.jhanp.psraerobicsbackend.auth.dto.response;

import java.util.List;

/**
 * Quién acaba de entrar. Va dentro de {@link AuthResponse} para que el front resuelva de una sola
 * llamada las tres cosas que necesita antes de pintar nada.
 * <p>
 * Es un DTO aparte de
 * {@link com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse} a propósito. Ese se usa
 * en listados paginados, y meterle roles y persona obligaría a resolver dos relaciones perezosas por
 * fila — un N+1 en cada página de usuarios. Aquí no cuesta nada: el login ya tiene la cuenta completa
 * en memoria, con roles y persona traídos por el JOIN FETCH de findByIdentifierWithRoles.
 */
public record AuthenticatedUserResponse(

        /** Id de la CUENTA. */
        Integer userId,

        /**
         * Id de la PERSONA, que es el que piden los endpoints de «lo mío»
         * (/api/personas/{id}/perfil, /saldo, /contacto-emergencia...). Null en cuentas técnicas:
         * un ADMIN puede existir sin persona detrás.
         */
        Integer personId,

        String documentNumber,

        /** Null si la cuenta se creó sin correo, que es el caso normal del kiosko. */
        String email,

        /** Nombre y apellidos, para saludar y para la cabecera. Null si no hay persona. */
        String fullName,

        /**
         * Las autoridades tal como las evalúa el backend: ADMIN, TEACHER, RECEPTIONIST, MEMBER. Son
         * las mismas cadenas que usan los @PreAuthorize, así que el front puede comparar contra ellas
         * sin traducir.
         * <p>
         * Es una LISTA porque el multirol es lo normal: la pregunta correcta es
         * {@code roles.includes('ADMIN')}, nunca {@code rol === 'ADMIN'}.
         */
        List<String> roles
) {
}
