package com.jhanp.psraerobicsbackend.modules.user.mapper;

import com.jhanp.psraerobicsbackend.modules.user.dto.response.TeacherResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Solo salida. No hay toEntity: la cuenta la crea sp_registrar_usuario_para_persona, que deriva el
 * número de documento de la persona asociada y asigna el rol MIEMBRO. No existe alta de usuario
 * suelta.
 */
@Mapper(componentModel = "spring", imports = PersonNames.class)
public interface UserMapper {

    /**
     * Los roles llegan aparte y no se leen de {@code user.getUserRoles()} a propósito: esa relación
     * es perezosa, y resolverla por cada fila de un listado es exactamente el N+1 que se quiere
     * evitar. Quien llama decide cómo los trae — en bloque para una página, sueltos para uno solo.
     */
    @Mapping(target = "roles", source = "roles")
    UserResponse toResponse(User user, List<String> roles);

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "person.id", target = "personId")
    @Mapping(target = "fullName", expression = "java(PersonNames.fullName(user.getPerson()))")
    TeacherResponse toTeacherResponse(User user);
}
