package com.jhanp.psraerobicsbackend.modules.aerobics.report.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DailyRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.SessionRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.view.DailyRevenueView;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.view.SessionRevenueView;
import org.mapstruct.Mapper;

/**
 * Pasa las dos vistas de recaudo a sus DTO. El id sintético de vista_recaudo_diario se queda en la
 * entidad: el DTO no lo declara, así que no puede filtrarse a la API.
 */
@Mapper(componentModel = "spring")
public interface RevenueViewMapper {

    DailyRevenueViewResponse toResponse(DailyRevenueView view);

    SessionRevenueViewResponse toResponse(SessionRevenueView view);
}
