package com.jhanp.psraerobicsbackend.modules.role.controller;

import com.jhanp.psraerobicsbackend.modules.role.dto.response.RoleResponse;
import com.jhanp.psraerobicsbackend.modules.role.enums.RoleName;
import com.jhanp.psraerobicsbackend.modules.role.service.interfaces.IRoleService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de roles del sistema (ADMIN, MIEMBRO, PROFESOR).
 * <p>
 * Solo lectura: los roles son datos semilla de la migración inicial.
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Validated
public class RoleController {

    private final IRoleService roleService;

    /**
     * Lista todos los roles disponibles en el sistema.
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    /**
     * Consulta un rol por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> findById(
            @PathVariable @Positive(message = "El ID del rol debe ser positivo") Integer id) {

        return ResponseEntity.ok(roleService.findById(id));
    }

    /**
     * Consulta un rol por su nombre enum.
     */
    @GetMapping("/nombre/{name}")
    public ResponseEntity<RoleResponse> findByName(
            @PathVariable RoleName name) {

        return ResponseEntity.ok(roleService.findByName(name));
    }
}
