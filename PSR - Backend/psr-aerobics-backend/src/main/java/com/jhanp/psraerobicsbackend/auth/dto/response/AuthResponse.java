package com.jhanp.psraerobicsbackend.auth.dto.response;

import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;

public record AuthResponse(
        String token,
        UserResponse user
) {}
