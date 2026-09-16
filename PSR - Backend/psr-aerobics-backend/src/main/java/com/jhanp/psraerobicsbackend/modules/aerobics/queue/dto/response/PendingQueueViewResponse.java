package com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;

import java.time.OffsetDateTime;

/** Respuesta pública de personas llamadas a recepción, sin información contable. */
public record PendingQueueViewResponse(
        Integer checkinId,
        Integer sessionId,
        Integer personId,
        String firstName,
        String lastName,
        OffsetDateTime arrivalTime,
        CheckinMethod checkinMethod,
        Long position,
        Boolean attended
) {
}