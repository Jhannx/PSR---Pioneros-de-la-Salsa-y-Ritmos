package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /api/personas y POST /api/personas/autoregistro.
 * <p>
 * Un solo DTO para los dos endpoints porque comparten el 100 % de los campos. Lo único que cambia es
 * fuente_registro, y eso lo decide la ruta, no el cliente: si viniera en el body, cualquiera podría
 * marcar un registro manual como autoregistro.
 * <p>
 * Toda persona registrada nace con cuenta de acceso, así que no hay un endpoint aparte para crearla.
 */
public record PersonRequest(
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
        String phone,

        // null → el backend hashea el número de documento como contraseña inicial (RF-01).
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password
) {
}
