package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Entrada de PUT /api/personas/{id}/observaciones-salud.
 * <p>
 * Un DTO para un solo campo, en vez de recibir el String suelto en el body: así el endpoint acepta
 * JSON como todos los demás ({@code {"healthObservations": "..."}}) y añadir mañana otro dato de
 * salud no rompe el contrato.
 * <p>
 * Sin @NotBlank a propósito: borrar lo que uno escribió es una operación legítima, así que null y
 * cadena vacía son válidos. El tope existe para no dejar la columna TEXT abierta a cualquier tamaño.
 */
public record HealthObservationsRequest(
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres")
        String healthObservations,

        /*
         * Numeral 4.2 de la política: el consentimiento expreso y diferenciado para el dato sensible.
         *
         * Sin @NotNull, y es la diferencia entre cumplir la ley y no cumplirla. Solo hace falta
         * cuando se envía contenido; vaciar el campo nunca lo exige, porque retirar un dato no
         * necesita permiso para tratarlo. Un @NotNull aquí obligaría a marcar una casilla de
         * autorización para poder BORRAR lo que uno escribió, que es exactamente al revés.
         *
         * Tampoco @AssertTrue: la validación depende del otro campo y vive en el service.
         */
        Boolean healthDataConsent
) {
}
