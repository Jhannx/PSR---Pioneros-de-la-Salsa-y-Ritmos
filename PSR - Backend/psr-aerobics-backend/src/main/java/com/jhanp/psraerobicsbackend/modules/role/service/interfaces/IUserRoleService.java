package com.jhanp.psraerobicsbackend.modules.role.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.user.dto.request.UserRoleRequest;
import com.jhanp.psraerobicsbackend.modules.role.dto.response.RoleResponse;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;

import java.util.List;

/**
 * Los tres endpoints de roles devuelven la lista completa de roles del usuario, no la fila de
 * usuario_rol: al frontend le sirve el estado resultante, y a Spring Security también — las
 * authorities se cargan como colección, porque usuario ya no tiene rol_id.
 */
public interface IUserRoleService {

    /**
     * Asigna un rol vía sp_asignar_rol. Idempotente gracias a uq_usuario_rol.
     * <p>
     * MIEMBRO, PROFESOR y RECEPCIONISTA exigen que el usuario tenga persona asociada
     * (trg_validar_persona_requerida); solo ADMIN puede ser una cuenta sin persona.
     */
    List<RoleResponse> assign(Integer userId, UserRoleRequest request);

    /**
     * Quita un rol vía sp_quitar_rol. Falla si dejaría al usuario sin ningún rol. El rol viaja en el
     * path (/api/usuarios/{id}/roles/{rol}), así que el DELETE no lleva body.
     */
    void revoke(Integer userId, RoleName roleName);

    List<RoleResponse> findByUser(Integer userId);

    boolean hasRole(Integer userId, RoleName roleName);
}
