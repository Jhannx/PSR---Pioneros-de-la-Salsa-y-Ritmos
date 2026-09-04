package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.entity.Attendance;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida. No hay toEntity: la fila de asistencia la crea sp_registrar_asistencia, que cobra la
 * tarifa vigente en el mismo INSERT. Construirla en Java sería replicar ese cobro.
 */
@Mapper(componentModel = "spring", imports = PersonNames.class)
public interface AttendanceMapper {

    @Mapping(source = "id", target = "attendanceId")
    @Mapping(source = "checkin.id", target = "checkinId")
    @Mapping(source = "checkin.person.id", target = "personId")
    @Mapping(target = "fullName",
            expression = "java(PersonNames.fullName(attendance.getCheckin().getPerson()))")
    @Mapping(source = "markedBy.id", target = "markedByUserId")
    @Mapping(target = "markedByName",
            expression = "java(PersonNames.fullName(attendance.getMarkedBy().getPerson()))")
    AttendanceResponse toResponse(Attendance attendance);
}
