package com.jhanp.psraerobicsbackend.modules.person.mapper;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonUpdateRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * No hay toEntity desde PersonRequest: el alta pasa por sp_registrar_persona, que fija
 * fecha_registro y activo por su cuenta. La edición sí es JPA, y se aplica sobre la fila existente.
 */
@Mapper(componentModel = "spring")
public interface PersonMapper {

    /**
     * userId no sale de Person (la FK vive en usuario): lo pasa el service, que acaba de crear la
     * cuenta en la misma transacción, o lo resuelve al leer.
     */
    @Mapping(source = "person.name", target = "firstName")
    @Mapping(source = "person.registrationSource", target = "source")
    @Mapping(source = "userId", target = "userId")
    PersonResponse toResponse(Person person, Integer userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "registrationSource", ignore = true)
    @Mapping(target = "emergencyContact", ignore = true)
    @Mapping(source = "firstName", target = "name")
    void updateEntity(PersonUpdateRequest request, @MappingTarget Person person);
}
