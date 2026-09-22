package com.jhanp.psraerobicsbackend.modules.role.controller;

import com.jhanp.psraerobicsbackend.modules.user.dto.request.UserRoleRequest;
import com.jhanp.psraerobicsbackend.modules.role.dto.response.RoleResponse;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.role.service.interfaces.IUserRoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import com.jhanp.psraerobicsbackend.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión de roles de usuario.
 */
@RestController
@RequestMapping("/api/usuarios/{userId}/roles")
// Quién puede qué en el sistema. Si esto no fuera solo de ADMIN, cualquiera podría ascenderse.
@PreAuthorize(Authorities.ADMIN)
@RequiredArgsConstructor
@Validated
public class UserRoleController {

    private final IUserRoleService userRoleService;

    /**
     * Asigna un rol a un usuario. Devuelve la lista completa de roles del usuario.
     */
    @PostMapping
    public ResponseEntity<List<RoleResponse>> assign(
            @PathVariable @Positive(message = "El ID de usuario debe ser positivo") Integer userId,
            @RequestBody @Valid UserRoleRequest request) {
        return ResponseEntity.ok(userRoleService.assign(userId, request));
    }

    /**
     * Revoca un rol a un usuario.
     */
    @DeleteMapping("/{roleName}")
    public ResponseEntity<Void> revoke(
            @PathVariable @Positive(message = "El ID de usuario debe ser positivo") Integer userId,
            @PathVariable RoleName roleName) {
        userRoleService.revoke(userId, roleName);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene todos los roles de un usuario.
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> findByUser(
            @PathVariable @Positive(message = "El ID de usuario debe ser positivo") Integer userId) {
        return ResponseEntity.ok(userRoleService.findByUser(userId));
    }
}
