package com.jhanp.psraerobicsbackend.modules.person.dto.response;

import com.jhanp.psraerobicsbackend.modules.person.enums.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Salida de GET /api/personas/{id}/perfil, desde vista_perfil_persona: datos, contacto de
 * emergencia, saldo, días de crédito y resumen de asistencias en una sola consulta.
 */
public record PersonProfileViewResponse(
        Integer personId,
        String fullName,
        DocumentType documentType,
        String documentNumber,
        String phone,
        Boolean active,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyObservations,
        BigDecimal balance,
        Integer availableCreditDays,
        Long totalAttendances,
        LocalDate lastAttendanceDate,
        OffsetDateTime lastAttendanceTime
) {
}
