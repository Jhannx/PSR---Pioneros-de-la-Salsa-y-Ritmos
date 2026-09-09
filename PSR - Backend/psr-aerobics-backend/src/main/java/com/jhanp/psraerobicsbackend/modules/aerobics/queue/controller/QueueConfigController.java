package com.jhanp.psraerobicsbackend.modules.aerobics.queue.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.request.QueueConfigRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.QueueConfigResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces.IQueueConfigService;
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
 * Configuración de la cola (fila única id = 1).
 * <p>
 * Regula personas por turno, margen de ingreso antes de la clase y minutos de visibilidad de pagos
 * en la sala de espera.
 */
@RestController
@RequestMapping("/api/cola/configuracion")
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
    public ResponseEntity<QueueConfigResponse> update(
            @Valid @RequestBody QueueConfigRequest request,
            Authentication authentication) {

        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(queueConfigService.update(request, userId));
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
