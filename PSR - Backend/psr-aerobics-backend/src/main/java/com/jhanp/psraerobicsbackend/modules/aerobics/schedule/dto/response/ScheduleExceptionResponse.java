package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response;

import java.time.LocalDate;

public record ScheduleExceptionResponse(
        Integer id,
        Integer scheduleId,
        LocalDate date,
        String reason
) {
}
