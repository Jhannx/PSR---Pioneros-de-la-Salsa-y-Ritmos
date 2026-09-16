package com.jhanp.psraerobicsbackend.modules.aerobics.queue.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.request.QueueConfigRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.QueueConfigResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces.IQueueConfigService;
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
 * Configuración de la cola (fila única id = 1).
 * <p>
 * Regula personas por turno, margen de ingreso antes de la clase y minutos de visibilidad de pagos
 * en la sala de espera.
 */
@RestController
@RequestMapping("/api/cola/configuracion")
// La sala necesita leerla para saber cuántos entran por turno; ajustarla es administración.
@PreAuthorize(Authorities.STAFF)
@RequiredArgsConstructor
@Validated
public class QueueConfigController {

    private final IQueueConfigService queueConfigService;

    /**
     * Consulta la configuración vigente de la cola.
     */
    @GetMapping
    public ResponseEntity<QueueConfigResponse> getCurrent() {
        return ResponseEntity.ok(queueConfigService.getCurrent());
    }

    /**
     * Actualiza los parámetros de configuración de la cola.
     * <p>
     * El usuario que realiza la actualización se obtiene del contexto de autenticación (JWT) para
     * registrar la auditoría.
     */
    @PutMapping
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<QueueConfigResponse> update(
            @Valid @RequestBody QueueConfigRequest request,
            Authentication authentication) {

        Integer userId = AuthenticatedUser.requireId(authentication);
        return ResponseEntity.ok(queueConfigService.update(request, userId));
    }
}
