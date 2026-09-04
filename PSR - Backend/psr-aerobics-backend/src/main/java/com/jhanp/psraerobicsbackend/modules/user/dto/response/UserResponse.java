package com.jhanp.psraerobicsbackend.modules.user.dto.response;

import java.time.OffsetDateTime;

public record UserResponse(
        Integer id,
        String documentNumber,
        String email,
        Boolean active,
        OffsetDateTime createdAt
) {
}
