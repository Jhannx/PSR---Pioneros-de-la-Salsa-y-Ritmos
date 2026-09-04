package com.jhanp.psraerobicsbackend.auth.dto.response;

import java.time.LocalDateTime;

public record RegisterResponse(
        Integer id,
        String name,
        String email,
        LocalDateTime creationDate) {
}
