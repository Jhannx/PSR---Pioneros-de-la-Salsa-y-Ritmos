package com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PaymentMethodRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PaymentMethodResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * CRUD plano: metodo_pago ya no es un enum sino una tabla, así que sí se construye desde el request.
 * active se mapea (no se ignora); si viene null, el @PrePersist de la entidad lo deja en true.
 */
@Mapper(componentModel = "spring")
public interface PaymentMethodMapper {

    @Mapping(target = "id", ignore = true)
    PaymentMethod toEntity(PaymentMethodRequest request);

    PaymentMethodResponse toResponse(PaymentMethod paymentMethod);
}
