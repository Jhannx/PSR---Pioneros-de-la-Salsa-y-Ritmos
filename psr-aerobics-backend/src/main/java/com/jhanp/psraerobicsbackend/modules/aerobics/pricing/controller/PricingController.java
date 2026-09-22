package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.request.PricingRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.response.PricingResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.service.interfaces.IPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.jhanp.psraerobicsbackend.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.jhanp.psraerobicsbackend.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de la tarifa vigente del sistema (fila única id = 1).
 */
@RestController
@RequestMapping("/api/tarifa")
// Consultarla la necesita todo el personal —sin saber el precio no se puede cobrar—; cambiarla no.
@PreAuthorize(Authorities.STAFF)
@RequiredArgsConstructor
@Validated
public class PricingController {

    private final IPricingService pricingService;

    /**
     * Consulta la tarifa vigente aplicada a las asistencias.
     * <p>
     * Abierto también a MIEMBRO y a la Terminal, no solo al personal: cuánto cuesta la clase no es
     * información interna, es el precio que la persona va a pagar. Su pantalla de inicio lo muestra,
     * y con el permiso heredado de la clase recibía un 403.
     * <p>
     * Sigue fuera del alcance de quien no ha iniciado sesión: el precio se ve estando dentro.
     */
    @GetMapping
    @PreAuthorize(Authorities.PERSON_LOOKUP + " or hasAuthority('MEMBER')")
    public ResponseEntity<PricingResponse> getCurrent() {
        return ResponseEntity.ok(pricingService.getCurrent());
    }

    /**
     * Actualiza el valor de la tarifa general.
     * <p>
     * El usuario que realiza la actualización se obtiene del contexto de autenticación (JWT) para
     * auditar el cambio.
     */
    @PutMapping
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<PricingResponse> update(
            @Valid @RequestBody PricingRequest request,
            Authentication authentication) {

        Integer userId = AuthenticatedUser.requireId(authentication);
        return ResponseEntity.ok(pricingService.update(request, userId));
    }

}
