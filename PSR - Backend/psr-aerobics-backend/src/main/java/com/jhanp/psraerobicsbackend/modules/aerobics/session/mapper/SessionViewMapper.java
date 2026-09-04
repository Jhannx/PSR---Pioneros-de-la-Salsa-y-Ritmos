package com.jhanp.psraerobicsbackend.modules.aerobics.session.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionSummaryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.view.SessionSummaryView;
import org.mapstruct.Mapper;

/**
 * Pasa vista_resumen_sesiones a su DTO. Va aparte de SessionMapper, que trabaja sobre la tabla
 * sesion y no sabe de totales ni del nombre del profesor.
 */
@Mapper(componentModel = "spring")
public interface SessionViewMapper {

    SessionSummaryViewResponse toResponse(SessionSummaryView view);
}
