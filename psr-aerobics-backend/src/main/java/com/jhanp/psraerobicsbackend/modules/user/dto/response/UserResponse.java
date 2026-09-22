package com.jhanp.psraerobicsbackend.modules.user.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record UserResponse(
        Integer id,
        String documentNumber,
        String email,
        Boolean active,
        OffsetDateTime createdAt,

        /**
         * Los roles de la cuenta, en inglés y sin prefijo ROLE_ (ADMIN, MEMBER, TEACHER,
         * RECEPTIONIST, PSR_TERMINAL) — la misma forma que usan el token y
         * GET /api/usuarios/{id}/roles.
         * <p>
         * Viaja aquí desde el 13-sep porque no estaba, y su ausencia obligaba a la pantalla de roles
         * y accesos a pedirlos cuenta por cuenta: una página de 20 usuarios eran 21 peticiones, y
         * cada una vuelve a resolver el usuario y sus roles en el filtro de autenticación. Esa
         * amplificación era la causa estructural de que la pantalla se sintiera lenta, y no se
         * arregla con un índice.
         */
        List<String> roles
) {
}
