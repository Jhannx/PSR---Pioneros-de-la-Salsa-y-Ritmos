package com.jhanp.psraerobicsbackend.modules.user.controller;

import com.jhanp.psraerobicsbackend.modules.user.dto.response.TeacherResponse;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta y estado de las cuentas de acceso.
 * <p>
 * No expone alta: la cuenta nace junto con la persona, en POST /api/personas. Tampoco cambio de
 * contraseña, que entra por el módulo auth con su token de recuperación.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final IUserService userService;

    /**
     * Profesores asignables a una sesión: usuarios activos con rol PROFESOR.
     * <p>
     * Va antes de /{id} para que la ruta literal gane sobre la variable de path.
     */
    @GetMapping("/profesores")
    public ResponseEntity<List<TeacherResponse>> findAssignableTeachers() {

        return ResponseEntity.ok(userService.findAssignableTeachers());
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(
            @PathVariable @Positive(message = "El ID de usuario debe ser positivo") Integer id) {

        return ResponseEntity.ok(userService.findById(id));
    }

    /**
     * La cuenta asociada a una persona. Devuelve 404 si esa persona no tiene cuenta.
     */
    @GetMapping("/persona/{personId}")
    public ResponseEntity<UserResponse> findByPersonId(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer personId) {

        return ResponseEntity.ok(userService.findByPersonId(personId));
    }

    /**
     * Reactiva la cuenta sin tocar a la persona. Para reactivar a la persona —que además propaga a
     * su cuenta y queda auditado— está PATCH /api/personas/{id}/reactivar.
     */
    @PatchMapping("/{id}/activar")
    public ResponseEntity<UserResponse> activate(
            @PathVariable @Positive(message = "El ID de usuario debe ser positivo") Integer id) {

        return ResponseEntity.ok(userService.setActive(id, true));
    }

    /**
     * Bloquea el acceso dejando a la persona activa: sirve para suspender una cuenta sin dar de baja
     * a quien asiste a clase.
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<UserResponse> deactivate(
            @PathVariable @Positive(message = "El ID de usuario debe ser positivo") Integer id) {

        return ResponseEntity.ok(userService.setActive(id, false));
    }
}
