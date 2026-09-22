package com.jhanp.psraerobicsbackend.modules.role.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.role.dto.response.RoleResponse;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;

import java.util.List;

/**
 * Catálogo de solo lectura: los tres roles son datos semilla de la migración inicial, no se crean
 * ni se borran desde la aplicación.
 */
public interface IRoleService {

    List<RoleResponse> findAll();

    RoleResponse findById(Integer id);

    RoleResponse findByName(RoleName name);
}
