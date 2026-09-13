package com.jhanp.psraerobicsbackend.security;

import com.jhanp.psraerobicsbackend.security.userdetails.model.CustomUserDetails;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import org.springframework.security.core.Authentication;

/**
 * Saca el id de usuario del contexto de seguridad. Lo usan los controllers cuyos procedimientos
 * auditan quién ejecutó la acción (reactivar persona, registrar un movimiento, cerrar una sesión...).
 * <p>
 * Antes cada controller tenía su propia copia de esta lógica, y todas estaban mal: intentaban leer el
 * principal como Integer —que nunca lo es, el principal es un {@link CustomUserDetails}— y si no,
 * hacían {@code Integer.parseInt(authentication.getName())}. Con eso, una cuenta con correo caía en un
 * {@code return 1} y atribuía todo al usuario 1; una cuenta sin correo era peor, porque
 * {@code getName()} devuelve el número de documento, el parseInt funcionaba, y se enviaba un documento
 * donde se esperaba un id de usuario: violación de clave foránea contra usuario(id).
 */
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    /**
     * El id de la cuenta autenticada.
     * <p>
     * Lanza si no hay nadie autenticado en vez de devolver un id por defecto: estas acciones quedan
     * registradas en historial_cambio, y atribuirlas a una cuenta que no las hizo es peor que
     * rechazarlas. En la práctica no debería ocurrir — las rutas que llaman aquí exigen token.
     */
    public static Integer requireId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser().getId();
        }
        throw new InvalidDataException("No se pudo determinar el usuario autenticado");
    }
}
