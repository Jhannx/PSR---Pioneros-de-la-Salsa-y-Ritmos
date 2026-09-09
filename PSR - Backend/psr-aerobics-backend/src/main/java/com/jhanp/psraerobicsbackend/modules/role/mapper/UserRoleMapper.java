package com.jhanp.psraerobicsbackend.modules.role.mapper;

import com.jhanp.psraerobicsbackend.modules.role.dto.response.RoleResponse;
import com.jhanp.psraerobicsbackend.modules.role.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Saca el rol de una fila de usuario_rol.
 * <p>
 * Los tres endpoints de roles devuelven la lista de roles del usuario, no la fila de la tabla
 * intermedia: assignedAt lo pone Postgres al insertar y no aparece en ningún JSON del contrato.
 */
@Mapper(componentModel = "spring")
public interface UserRoleMapper {

    @Mapping(source = "role.id", target = "id")
    @Mapping(source = "role.name", target = "name")
    RoleResponse toRoleResponse(UserRole userRole);
}
