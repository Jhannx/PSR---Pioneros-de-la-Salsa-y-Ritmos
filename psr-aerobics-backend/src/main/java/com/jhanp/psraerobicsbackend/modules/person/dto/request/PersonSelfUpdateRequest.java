package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Entrada de PUT /api/personas/{id}/mis-datos — lo que la propia persona puede corregir de sí misma.
 * <p>
 * <b>Lo que no está aquí es la razón de que exista.</b> {@link PersonUpdateRequest} lleva también
 * tipo y número de documento, y por eso su endpoint es de mostrador: abrirlo al dueño le dejaría
 * cambiarse el número con el que se identifica ante el sistema. Este DTO no los declara, así que la
 * regla no depende de que nadie se acuerde de comprobarla — no hay campo que ignorar.
 * <p>
 * Mismo reparto que las observaciones de salud: un endpoint por conjunto de datos, y los permisos
 * salen de qué se puede tocar, no de quién pregunta.
 */
public record PersonSelfUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String firstName,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
        String lastName,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        String phone,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate birthDate
) {
}
