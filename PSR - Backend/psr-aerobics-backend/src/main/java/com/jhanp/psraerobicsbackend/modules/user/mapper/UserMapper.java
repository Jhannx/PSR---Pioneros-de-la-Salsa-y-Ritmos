package com.jhanp.psraerobicsbackend.modules.user.mapper;

import com.jhanp.psraerobicsbackend.modules.user.dto.response.TeacherResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida. No hay toEntity: la cuenta la crea sp_registrar_usuario_para_persona, que deriva el
 * número de documento de la persona asociada y asigna el rol MIEMBRO. No existe alta de usuario
 * suelta.
 */
@Mapper(componentModel = "spring", imports = PersonNames.class)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(source = "id", target = "userId")
    @Mapping(source = "person.id", target = "personId")
    @Mapping(target = "fullName", expression = "java(PersonNames.fullName(user.getPerson()))")
    TeacherResponse toTeacherResponse(User user);
}
