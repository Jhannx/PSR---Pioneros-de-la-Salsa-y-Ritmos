package com.jhanp.psraerobicsbackend.modules.role.dto.response;

import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;

public record RoleResponse(
        Integer id,
        RoleName name
) {
}
