package com.jhanp.psraerobicsbackend.modules.aerobics.queue.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.QueueConfigResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.entity.QueueConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida. No hay toEntity: configuracion_cola es una fila única que siempre se actualiza sobre
 * la existente (id = 1), nunca se construye una instancia nueva desde el request.
 */
@Mapper(componentModel = "spring")
public interface QueueConfigMapper {

    @Mapping(source = "personsPerTurn", target = "peoplePerTurn")
    @Mapping(source = "updatedBy.id", target = "updatedByUserId")
    QueueConfigResponse toResponse(QueueConfig queueConfig);
}
