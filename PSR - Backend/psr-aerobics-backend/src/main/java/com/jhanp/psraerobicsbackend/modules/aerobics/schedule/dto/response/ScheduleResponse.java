package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response;

import java.time.LocalTime;

public record ScheduleResponse(
        Integer id,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Boolean active
) {
}
