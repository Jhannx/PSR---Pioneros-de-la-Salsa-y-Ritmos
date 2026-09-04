package com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.AccountMovementResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.AccountMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida. No hay toEntity: abonos y reembolsos los inserta el procedimiento, que valida la
 * persona activa y el tope contra el monto original. La corrección (PATCH) sí es JPA, pero se
 * aplica campo por campo sobre la fila existente, no construyendo una entidad nueva.
 */
@Mapper(componentModel = "spring")
public interface AccountMovementMapper {

    @Mapping(source = "person.id", target = "personId")
    @Mapping(source = "checkin.id", target = "checkinId")
    @Mapping(source = "amountCredited", target = "amountApplied")
    @Mapping(source = "paymentMethod.id", target = "paymentMethodId")
    @Mapping(source = "paymentMethod.name", target = "paymentMethodName")
    @Mapping(source = "originalMovement.id", target = "originalMovementId")
    @Mapping(source = "timestamp", target = "dateTime")
    @Mapping(source = "user.id", target = "userId")
    AccountMovementResponse toResponse(AccountMovement accountMovement);
}
