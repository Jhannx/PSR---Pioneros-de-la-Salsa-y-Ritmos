package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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

        /**
         * Obligatoria en las altas nuevas, aunque la columna sea nullable: la base tiene que admitir
         * null por las personas registradas antes de que este campo existiera, y este DTO es el único
         * sitio donde se puede distinguir un alta nueva de una fila histórica.
         * <p>
         * @Past y no @PastOrPresent: nadie se inscribe a aeróbicos el día que nace.
         */
        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate birthDate,

        /**
         * Opcional. Se guarda en usuario.correo, no en persona: es un dato de la cuenta, y es lo que
         * permite entrar por correo además de por documento y recibir el aviso de alta. Sin él la
         * cuenta funciona igual, solo que se identifica únicamente por el número de documento.
         */
        @Email(message = "El formato del correo no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String email,

        // null → el backend hashea el número de documento como contraseña inicial (RF-01).
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password
) {
}
