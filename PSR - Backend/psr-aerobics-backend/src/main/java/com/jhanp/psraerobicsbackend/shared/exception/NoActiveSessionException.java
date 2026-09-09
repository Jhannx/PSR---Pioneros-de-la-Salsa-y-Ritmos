package com.jhanp.psraerobicsbackend.shared.exception;

/**
 * SQLSTATE 75002 — no hay ninguna clase vigente en este momento. Lo lanza
 * fn_obtener_o_crear_sesion_del_momento cuando ningún horario aplica.
 * <p>
 * No es un error del usuario: el frontend solo muestra "No hay clase ahora". Por eso va separado de
 * la persona desactivada, aunque ambos respondan 409.
 */
public class NoActiveSessionException extends RuntimeException {

    public static final String CODE = "NO_ACTIVE_SESSION";

    public NoActiveSessionException(String message) {
        super(message);
    }
}
