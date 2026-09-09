package com.jhanp.psraerobicsbackend.shared.exception;

/**
 * SQLSTATE 75001 — la persona existe pero está desactivada. Lo lanza fn_validar_persona_activa.
 * <p>
 * Es distinto de "no existe": el frontend debe ofrecer "Usuario desactivado, ¿deseas reactivarlo?"
 * y llamar a PATCH /api/personas/{id}/reactivar. Por eso no puede mezclarse con un 404.
 */
public class DeactivatedPersonException extends RuntimeException {

    public static final String CODE = "PERSON_DEACTIVATED";

    public DeactivatedPersonException(String message) {
        super(message);
    }
}
