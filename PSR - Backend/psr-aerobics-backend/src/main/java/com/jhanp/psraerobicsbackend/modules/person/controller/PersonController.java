package com.jhanp.psraerobicsbackend.modules.person.controller;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonUpdateRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonProfileViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IPersonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alta, consulta y baja de personas. Toda persona registrada nace con cuenta de acceso, así que no
 * hay un endpoint aparte para crearla.
 */
@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
@Validated
public class PersonController {

    private final IPersonService personService;

    /**
     * Listado paginado. Con ?buscar= filtra por nombre, apellidos o número de documento.
     */
    @GetMapping
    public ResponseEntity<Page<PersonResponse>> findAll(
            @RequestParam(required = false) String buscar,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PersonResponse> personas = (buscar == null || buscar.isBlank())
                ? personService.findAll(pageable)
                : personService.search(buscar, pageable);

        return ResponseEntity.ok(personas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> findById(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(personService.findById(id));
    }

    /**
     * Registro hecho por un administrador.
     * <p>
     * La fuente de registro la fija la ruta y nunca el body: si viniera del cliente, cualquiera
     * podría marcar un registro manual como autoregistro.
     */
    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personService.create(request, RegistrationSource.ADMIN));
    }

    /**
     * Registro que hace la propia persona desde el kiosko.
     */
    @PostMapping("/autoregistro")
    public ResponseEntity<PersonResponse> selfRegister(@Valid @RequestBody PersonRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(personService.create(request, RegistrationSource.SELF_REGISTRATION));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonResponse> update(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @Valid @RequestBody PersonUpdateRequest request) {

        return ResponseEntity.ok(personService.update(id, request));
    }

    /**
     * Desactiva a la persona. El trigger de sincronización desactiva también su cuenta.
     */
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Void> deactivate(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        personService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva a la persona. Queda auditado, por eso hace falta saber quién lo hizo.
     * <p>
     * El usuario que reactiva sale del contexto de autenticación (JWT).
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<PersonResponse> reactivate(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            Authentication authentication) {

        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(personService.reactivate(id, userId));
    }

    /**
     * Eliminación definitiva e irreversible: borra persona y cuenta, y reasigna su huella
     * (llegadas, movimientos, asistencias) al registro sentinel.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            Authentication authentication) {

        Integer userId = resolveUserId(authentication);
        personService.deletePermanently(id, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Perfil consolidado: datos, contacto de emergencia, saldo, días de crédito y resumen de
     * asistencias en una sola consulta.
     */
    @GetMapping("/{id}/perfil")
    public ResponseEntity<PersonProfileViewResponse> getProfile(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(personService.getProfile(id));
    }

    /**
     * Saldo a favor. Negativo significa que la persona debe.
     */
    @GetMapping("/{id}/saldo")
    public ResponseEntity<PersonBalanceViewResponse> getBalance(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(personService.getBalance(id));
    }

    private Integer resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Integer uid) {
            return uid;
        }
        if (authentication != null) {
            try {
                return Integer.parseInt(authentication.getName());
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }
}
