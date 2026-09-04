package com.jhanp.psraerobicsbackend.modules.user.dto.request;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada de POST /api/usuarios/{id}/roles.
 * <p>
 * Lleva el nombre del rol y no su id porque sp_asignar_rol recibe un rol_nombre_enum: mandar el id
 * obligaría a resolver id → nombre antes de cada llamada, por nada.
 * <p>
 * userId no va en el body: viaja en el path. Si viniera del cliente se podrían asignar roles a
 * nombre de otro usuario.
 */
public record UserRoleRequest(
        @NotNull(message = "El nombre del rol es obligatorio")
        RoleName roleName
) {
}
