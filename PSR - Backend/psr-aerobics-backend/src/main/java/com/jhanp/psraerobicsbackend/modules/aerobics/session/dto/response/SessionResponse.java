package com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SessionResponse(
        Integer id,
        Integer scheduleId,
        LocalDate date,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer teacherUserId,
        Boolean cancelled
) {
}
