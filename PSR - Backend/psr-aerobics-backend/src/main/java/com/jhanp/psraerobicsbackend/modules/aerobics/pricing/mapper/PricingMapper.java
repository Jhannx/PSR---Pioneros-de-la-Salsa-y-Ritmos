package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.response.PricingResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.entity.Pricing;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida. No hay toEntity: tarifa es una fila única (id = 1) que siempre se actualiza sobre la
 * existente, nunca se construye desde el request.
 */
@Mapper(componentModel = "spring", imports = PersonNames.class)
public interface PricingMapper {

    @Mapping(source = "updatedBy.id", target = "updatedByUserId")
    @Mapping(target = "updatedByName",
            expression = "java(pricing.getUpdatedBy() == null ? null : PersonNames.fullName(pricing.getUpdatedBy().getPerson()))")
    PricingResponse toResponse(Pricing pricing);
}
