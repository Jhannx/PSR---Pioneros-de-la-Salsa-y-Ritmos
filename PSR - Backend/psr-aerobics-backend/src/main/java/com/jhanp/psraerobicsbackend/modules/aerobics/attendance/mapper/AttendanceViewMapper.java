package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.view.AttendanceBalanceView;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.view.AttendanceHistoryView;
import org.mapstruct.Mapper;

/**
 * Pasa las entidades de las vistas de asistencia a sus DTO. Va aparte de AttendanceMapper porque
 * aquel trabaja sobre la tabla asistencia y estas son vistas de solo lectura.
 * <p>
 * Los campos coinciden 1:1, salvo checkinMethod: la vista lo entrega como texto y el DTO lo expone
 * como enum. MapStruct hace esa conversión sola.
 */
@Mapper(componentModel = "spring")
public interface AttendanceViewMapper {

    AttendanceHistoryViewResponse toResponse(AttendanceHistoryView view);

    AttendanceBalanceViewResponse toResponse(AttendanceBalanceView view);
}
