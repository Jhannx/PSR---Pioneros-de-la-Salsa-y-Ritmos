package com.jhanp.psraerobicsbackend.modules.user.dto.response;

/**
 * Salida de GET /api/usuarios/profesores: puebla el select de "profesor" al crear una sesión.
 * <p>
 * DTO dedicado y no reutilización de {@link UserResponse}: el selector necesita el nombre completo,
 * que UserResponse no trae, y no debe exponer correo, documento ni estado de la cuenta.
 */
public record TeacherResponse(
        Integer userId,
        Integer personId,
        String fullName
) {
}
