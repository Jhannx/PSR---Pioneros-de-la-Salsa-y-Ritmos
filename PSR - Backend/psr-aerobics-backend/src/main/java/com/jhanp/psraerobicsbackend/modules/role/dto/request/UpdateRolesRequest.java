package com.jhanp.psraerobicsbackend.modules.role.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateRolesRequest(
        @NotEmpty(message = "Debe indicar al menos un rol")
        List<String> roles
) {}
