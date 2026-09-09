package com.jhanp.psraerobicsbackend.modules.person.mapper;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;
import com.jhanp.psraerobicsbackend.modules.person.entity.EmergencyContact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * JPA plano: el PUT es un upsert manual (buscar por persona_id y, si no existe, crear), así que hace
 * falta tanto construir la entidad como aplicar el request sobre una existente.
 */
@Mapper(componentModel = "spring")
public interface EmergencyContactMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EmergencyContact toEntity(EmergencyContactRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(EmergencyContactRequest request, @MappingTarget EmergencyContact contact);

    @Mapping(source = "person.id", target = "personId")
    EmergencyContactResponse toResponse(EmergencyContact emergencyContact);
}
