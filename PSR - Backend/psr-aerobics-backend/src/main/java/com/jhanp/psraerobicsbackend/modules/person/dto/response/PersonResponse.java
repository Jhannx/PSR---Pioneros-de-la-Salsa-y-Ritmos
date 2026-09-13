package com.jhanp.psraerobicsbackend.modules.person.dto.response;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Salida de los endpoints de persona.
 * <p>
 * sp_registrar_persona solo devuelve el id por OUT: source, registeredAt y active salen de releer la
 * fila después del CALL (patrón H). userId y email son de la CUENTA — no cuelgan de la entidad
 * Person, los aporta el service.
 */
public record PersonResponse(
        Integer id,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String phone,

        /** Null en las personas registradas antes de que existiera el campo. */
        LocalDate birthDate,

        /**
         * Solo lectura por aquí. Se escribe con PUT /api/personas/{id}/observaciones-salud, que tiene
         * su propio permiso: el PUT de persona es de mostrador y reescribe documento, nombre y
         * teléfono, así que abrirlo al propio miembro para que editara una nota de salud le dejaría
         * también cambiarse el documento.
         */
        String healthObservations,

        RegistrationSource source,
        OffsetDateTime registeredAt,
        Boolean active,
        Integer userId,

        /**
         * Correo de la cuenta, o null si se registró solo con documento. Se devuelve para que quien
         * acaba de dar de alta a alguien pueda confirmar que quedó bien escrito: es la dirección a la
         * que salió el correo de bienvenida.
         */
        String email
) {
}
