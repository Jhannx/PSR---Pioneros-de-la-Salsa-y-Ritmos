package com.jhanp.psraerobicsbackend.modules.aerobics.pricing.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.request.PricingRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.dto.response.PricingResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.pricing.service.interfaces.IPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequiredArgsConstructor
@Validated
public class PricingController {

    private final IPricingService pricingService;

    /**
     * Consulta la tarifa vigente aplicada a las asistencias.
     */
    @GetMapping
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
    public ResponseEntity<PricingResponse> update(
            @Valid @RequestBody PricingRequest request,
            Authentication authentication) {

        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(pricingService.update(request, userId));
    }

    private Integer resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Integer id) {
            return id;
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
