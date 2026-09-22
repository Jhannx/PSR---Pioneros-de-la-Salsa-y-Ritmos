package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleExceptionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleExceptionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.ScheduleException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScheduleExceptionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "schedule", ignore = true)
    ScheduleException toEntity(ScheduleExceptionRequest request);

    @Mapping(source = "schedule.id", target = "scheduleId")
    ScheduleExceptionResponse toResponse(ScheduleException scheduleException);
}
