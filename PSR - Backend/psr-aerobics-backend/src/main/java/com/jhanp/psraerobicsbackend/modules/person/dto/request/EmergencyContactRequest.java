package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Entrada de PUT /api/personas/{id}/contacto-emergencia. El PUT crea la fila si no existía.
 * <p>
 * personId no va en el body: viaja en el path. Al menos uno de los tres campos debe venir
 * (chk_contacto_emergencia_no_vacio).
 */
public record EmergencyContactRequest(
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String name,

        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone,

        String observations
) {
}
