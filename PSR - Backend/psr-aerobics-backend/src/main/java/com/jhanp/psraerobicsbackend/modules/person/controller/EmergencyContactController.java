package com.jhanp.psraerobicsbackend.modules.person.controller;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IEmergencyContactService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contacto de emergencia de una persona. Va anidado bajo /api/personas porque la relación es 1:1 y
 * el contacto no existe por sí solo.
 */
@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
@Validated
public class EmergencyContactController {

    private final IEmergencyContactService emergencyContactService;

    @GetMapping("/{id}/contacto-emergencia")
    public ResponseEntity<EmergencyContactResponse> findByPersonId(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(emergencyContactService.findByPersonId(id));
    }

    /**
     * Upsert: crea el contacto si la persona no tenía, o lo actualiza si ya existía. Por eso es PUT
     * y no POST, y por eso devuelve 200 en ambos casos.
     */
    @PutMapping("/{id}/contacto-emergencia")
    public ResponseEntity<EmergencyContactResponse> save(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @Valid @RequestBody EmergencyContactRequest request) {

        return ResponseEntity.ok(emergencyContactService.save(id, request));
    }
}
