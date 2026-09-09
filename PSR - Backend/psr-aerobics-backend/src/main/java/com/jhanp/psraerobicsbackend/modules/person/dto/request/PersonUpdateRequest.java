package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada de PUT /api/personas/{id} — solo datos básicos.
 * <p>
 * DTO dedicado pese al alto solapamiento con {@link PersonRequest}: no lleva password (cambiarla es
 * otro flujo), ni source, registeredAt o active, que no se editan por aquí. Reutilizar el DTO del
 * alta haría creer que un PUT puede reasignar la contraseña o la fuente de registro.
 */
public record PersonUpdateRequest(
        @NotNull(message = "El tipo de documento es obligatorio")
        DocumentType documentType,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres")
        String documentNumber,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String firstName,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String lastName,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone
) {
}
