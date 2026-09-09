package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    Schedule toEntity(ScheduleRequest request);

    ScheduleResponse toResponse(Schedule schedule);
}
