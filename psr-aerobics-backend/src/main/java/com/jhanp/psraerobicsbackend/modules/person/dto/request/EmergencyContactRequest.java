package com.jhanp.psraerobicsbackend.modules.person.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada de POST /api/personas/{id}/contactos-emergencia y de
 * PUT /api/contactos-emergencia/{contactoId}.
 * <p>
 * personId no va en el body: viaja en el path al crear, y al editar sale del contacto que se está
 * modificando. Un contacto nunca cambia de persona.
 * <p>
 * Ya no lleva observaciones: son de la persona ({@code PUT /api/personas/{id}/observaciones-salud}),
 * no de un contacto puntual. Con hasta cinco contactos, una observación de salud pegada a uno solo
 * no tendría dueño claro.
 * <p>
 * <b>El teléfono es obligatorio.</b> chk_contacto_emergencia_no_vacio solo exige que algún campo no
 * sea nulo —con el nombre bastaba—, así que se podían crear contactos sin forma de llamarlos, que es
 * justo lo único para lo que sirve un contacto de emergencia. La regla de verdad vive aquí.
 */
public record EmergencyContactRequest(
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String name,

        /*
         * Patrón deliberadamente permisivo: dígitos, espacios y prefijo internacional opcional. Cubre
         * el móvil colombiano de 10 dígitos, el fijo con indicativo y los números de fuera del país.
         * Exigir 10 dígitos exactos dejaría fuera casos válidos, y un contacto de emergencia que no se
         * puede registrar es peor fallo que el que esta validación arregla.
         *
         * El máximo del patrón y el del @Size tienen que coincidir; si divergen, uno de los dos
         * mensajes no se ve nunca.
         */
        @NotBlank(message = "El teléfono del contacto de emergencia es obligatorio")
        @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
        @Pattern(regexp = "^\\+?[0-9 ]{7,20}$",
                message = "El teléfono debe tener entre 7 y 20 dígitos, con prefijo internacional opcional")
        String phone,

        /*
         * Numeral 5 de la política: "declaro que estoy autorizado para suministrar estos datos para
         * esta finalidad, o que dispongo de una base legítima para hacerlo".
         *
         * Lo que se guarda no es el consentimiento del contacto —esa persona nunca habló con el
         * sistema y no se le puede pedir nada— sino la declaración de quien entrega sus datos. Es la
         * única base sobre la que se pueden tratar, y por eso hace falta constancia de ella.
         *
         * Sin @NotNull aunque al crear sea obligatorio: este record lo comparten el POST que crea y
         * el PUT que edita, y solo el primero lo exige. Corregir un dígito mal tecleado de un
         * teléfono no es suministrar datos de nadie nuevo, y volver a pedir la declaración en cada
         * arreglo la convertiría en un trámite que se marca sin leer. La regla vive en
         * EmergencyContactService.create.
         */
        Boolean thirdPartyConsent
) {
}
