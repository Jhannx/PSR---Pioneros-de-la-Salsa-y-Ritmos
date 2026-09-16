package com.jhanp.psraerobicsbackend.modules.person.dto.response;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Salida de GET /api/personas/{id}/perfil, desde vista_perfil_persona: datos, saldo, días de crédito
 * y resumen de asistencias en una sola consulta.
 * <p>
 * <b>Ya no trae el contacto de emergencia.</b> Al pasar los contactos a 1:N, el JOIN que los traía
 * devolvía esta misma persona una vez por contacto — y con ella su saldo y su conteo de asistencias
 * duplicados. La lista se pide a GET /api/personas/{id}/contactos-emergencia, que es donde una
 * lista sí cabe.
 */
public record PersonProfileViewResponse(
        Integer personId,
        String fullName,
        DocumentType documentType,
        String documentNumber,
        String phone,
        Boolean active,

        /** Puede ser null: las personas registradas antes de que existiera el campo no lo tienen. */
        LocalDate birthDate,

        /**
         * Alergias y condiciones de la PERSONA. Antes vivía en el contacto de emergencia, cuando
         * había uno solo; con varios no tendría dueño claro.
         */
        String healthObservations,

        BigDecimal balance,
        Integer availableCreditDays,
        Long totalAttendances,
        LocalDate lastAttendanceDate,
        OffsetDateTime lastAttendanceTime
) {
}
