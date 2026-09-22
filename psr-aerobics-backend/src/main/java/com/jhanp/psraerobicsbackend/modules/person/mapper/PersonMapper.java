package com.jhanp.psraerobicsbackend.modules.person.mapper;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonSelfUpdateRequest;
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
     * userId y email no salen de Person (viven en usuario): los pasa el service, que acaba de crear la
     * cuenta en la misma transacción, o los resuelve al leer.
     */
    @Mapping(source = "person.name", target = "firstName")
    @Mapping(source = "person.registrationSource", target = "source")
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "email", target = "email")
    PersonResponse toResponse(Person person, Integer userId, String email);

    /**
     * healthObservations se ignora a propósito: no está en PersonUpdateRequest y se escribe por su
     * propio endpoint. Si algún día se añadiera al request, quitar esta línea — mientras tanto evita
     * que MapStruct lo ponga a null en cada edición.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "registrationSource", ignore = true)
    @Mapping(target = "emergencyContacts", ignore = true)
    @Mapping(target = "healthObservations", ignore = true)
    @Mapping(source = "firstName", target = "name")
    void updateEntity(PersonUpdateRequest request, @MappingTarget Person person);

    /**
     * La edición que hace la propia persona. Documento y tipo de documento se ignoran <b>porque no
     * existen en el request</b>: sin estas dos líneas MapStruct los pondría a null en cada guardado, y
     * un miembro editando su teléfono se quedaría sin documento.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registeredAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "registrationSource", ignore = true)
    @Mapping(target = "emergencyContacts", ignore = true)
    @Mapping(target = "healthObservations", ignore = true)
    @Mapping(target = "documentType", ignore = true)
    @Mapping(target = "documentNumber", ignore = true)
    @Mapping(source = "firstName", target = "name")
    void updateOwnData(PersonSelfUpdateRequest request, @MappingTarget Person person);
}
