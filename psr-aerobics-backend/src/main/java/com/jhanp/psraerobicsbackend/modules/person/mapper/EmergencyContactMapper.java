package com.jhanp.psraerobicsbackend.modules.person.mapper;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;
import com.jhanp.psraerobicsbackend.modules.person.entity.EmergencyContact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * JPA plano. Hacen falta las dos direcciones porque los cuatro endpoints se reparten el trabajo:
 * POST construye una entidad nueva, PUT aplica el request sobre una que ya existe.
 * <p>
 * person se ignora en ambas: un contacto nunca cambia de persona. Al crear lo fija el service desde
 * el personId del path; al editar se deja como está — si el request pudiera tocarlo, un PUT sobre
 * /api/contactos-emergencia/{id} permitiría mover el contacto a otra ficha.
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
