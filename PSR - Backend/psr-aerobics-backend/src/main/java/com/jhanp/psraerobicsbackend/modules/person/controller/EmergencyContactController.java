package com.jhanp.psraerobicsbackend.modules.person.controller;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IEmergencyContactService;
import com.jhanp.psraerobicsbackend.security.Authorities;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contactos de emergencia. Hasta cinco por persona, sin orden de prioridad.
 * <p>
 * <b>Sin @RequestMapping de clase, y no es descuido.</b> Los cuatro endpoints viven en dos raíces
 * distintas, y eso refleja una diferencia real: listar y crear cuelgan de la persona
 * (/api/personas/{id}/contactos-emergencia) porque sin saber de quién son no significan nada;
 * editar y borrar cuelgan del contacto (/api/contactos-emergencia/{contactoId}) porque el id ya lo
 * identifica por sí solo. Cuando la relación era 1:1 esta distinción no existía: personId era
 * también la dirección del contacto.
 * <p>
 * Ojo con los permisos: los dos primeros usan OR_SELF sobre el id de persona; los dos últimos NO
 * pueden hacerlo —lo que llevan en la ruta es un id de contacto— y usan OR_OWNS_CONTACT, que resuelve
 * antes de quién es. Ver {@link Authorities#OR_OWNS_CONTACT}.
 */
@RestController
// Dato de seguridad de la sala: a quién llamar si alguien se descompensa. Todo el personal, y cada
// persona el suyo.
@PreAuthorize(Authorities.STAFF)
@RequiredArgsConstructor
@Validated
public class EmergencyContactController {

    private final IEmergencyContactService emergencyContactService;

    /** Lista vacía si la persona no tiene ninguno; 404 solo si la persona no existe. */
    @GetMapping("/api/personas/{id}/contactos-emergencia")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_SELF)
    public ResponseEntity<List<EmergencyContactResponse>> findByPersonId(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(emergencyContactService.findByPersonId(id));
    }

    /** 201 con el contacto creado, o 409 si la persona ya tiene cinco. */
    @PostMapping("/api/personas/{id}/contactos-emergencia")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_SELF)
    public ResponseEntity<EmergencyContactResponse> create(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @Valid @RequestBody EmergencyContactRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emergencyContactService.create(id, request));
    }

    /**
     * Edita nombre y teléfono de un contacto puntual. No puede moverlo a otra persona: el request no
     * lleva personId y el mapper ignora la relación.
     */
    @PutMapping("/api/contactos-emergencia/{contactId}")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_OWNS_CONTACT)
    public ResponseEntity<EmergencyContactResponse> update(
            @PathVariable @Positive(message = "El ID de contacto debe ser positivo") Integer contactId,
            @Valid @RequestBody EmergencyContactRequest request) {

        return ResponseEntity.ok(emergencyContactService.update(contactId, request));
    }

    /** 204. Una persona puede quedarse sin ningún contacto: es deseable tenerlos, no obligatorio. */
    @DeleteMapping("/api/contactos-emergencia/{contactId}")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_OWNS_CONTACT)
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "El ID de contacto debe ser positivo") Integer contactId) {

        emergencyContactService.delete(contactId);
        return ResponseEntity.noContent().build();
    }
}
