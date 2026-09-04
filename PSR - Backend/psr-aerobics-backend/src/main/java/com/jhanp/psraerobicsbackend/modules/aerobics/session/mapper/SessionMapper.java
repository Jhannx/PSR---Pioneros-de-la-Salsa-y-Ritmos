package com.jhanp.psraerobicsbackend.modules.aerobics.session.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.entity.Session;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida. No hay toEntity: toda sesión nace de un procedimiento — sp_crear_sesion_manual en el
 * alta del admin, o fn_obtener_o_crear_sesion_del_momento() en el check-in.
 */
@Mapper(componentModel = "spring")
public interface SessionMapper {

    @Mapping(source = "schedule.id", target = "scheduleId")
    @Mapping(source = "teacher.id", target = "teacherUserId")
    SessionResponse toResponse(Session session);
}
