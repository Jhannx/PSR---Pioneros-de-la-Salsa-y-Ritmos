package com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PendingChangeViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.view.PendingChangeView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Pasa vista_vueltos_pendientes a su DTO.
 * <p>
 * amountCredited (columna monto_abonado) sale como amountApplied, igual que en
 * AccountMovementResponse: es el mismo concepto y el frontend lo lee con ese nombre.
 */
@Mapper(componentModel = "spring")
public interface PendingChangeViewMapper {

    @Mapping(source = "amountCredited", target = "amountApplied")
    PendingChangeViewResponse toResponse(PendingChangeView view);
}
