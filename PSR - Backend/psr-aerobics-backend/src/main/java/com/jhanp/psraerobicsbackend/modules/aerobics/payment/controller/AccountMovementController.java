package com.jhanp.psraerobicsbackend.modules.aerobics.payment.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementPatchRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.RefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.AccountMovementResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PendingChangeViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces.IAccountMovementService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión de movimientos de cuenta (abonos, reembolsos, vueltos).
 */
@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
@Validated
public class AccountMovementController {

    private final IAccountMovementService accountMovementService;

    /**
     * Registro de un abono. El usuario que lo registra sale del JWT.
     */
    @PostMapping
    public ResponseEntity<AccountMovementResponse> registerDeposit(
            @RequestBody @Valid AccountMovementRequest request,
            Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountMovementService.registerDeposit(request, userId));
    }

    /**
     * Registro de un reembolso asociado a un abono original.
     */
    @PostMapping("/{id}/reembolso")
    public ResponseEntity<AccountMovementResponse> registerRefund(
            @PathVariable @Positive(message = "El ID de movimiento debe ser positivo") Integer id,
            @RequestBody @Valid RefundRequest request,
            Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountMovementService.registerRefund(id, request, userId));
    }

    /**
     * Corrección de un movimiento (monto o método mal digitado). Auditado por trigger.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AccountMovementResponse> correct(
            @PathVariable @Positive(message = "El ID de movimiento debe ser positivo") Integer id,
            @RequestBody @Valid AccountMovementPatchRequest request,
            Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(accountMovementService.correct(id, request, userId));
    }

    /**
     * Entrega de un vuelto pendiente.
     */
    @PostMapping("/{id}/vuelto")
    public ResponseEntity<AccountMovementResponse> deliverChange(
            @PathVariable @Positive(message = "El ID de movimiento debe ser positivo") Integer id,
            Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        return ResponseEntity.ok(accountMovementService.deliverChange(id, userId));
    }

    /**
     * Obtener un movimiento por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountMovementResponse> findById(
            @PathVariable @Positive(message = "El ID de movimiento debe ser positivo") Integer id) {
        return ResponseEntity.ok(accountMovementService.findById(id));
    }

    /**
     * Movimientos de una persona.
     */
    @GetMapping("/persona/{personId}")
    public ResponseEntity<Page<AccountMovementResponse>> findByPerson(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer personId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(accountMovementService.findByPerson(personId, pageable));
    }

    /**
     * Movimientos asociados a una llegada.
     */
    @GetMapping("/checkin/{checkinId}")
    public ResponseEntity<List<AccountMovementResponse>> findByCheckin(
            @PathVariable @Positive(message = "El ID de llegada debe ser positivo") Integer checkinId) {
        return ResponseEntity.ok(accountMovementService.findByCheckin(checkinId));
    }

    /**
     * Vueltos pendientes de entrega.
     */
    @GetMapping("/pendientes/vueltos")
    public ResponseEntity<List<PendingChangeViewResponse>> findPendingChanges() {
        return ResponseEntity.ok(accountMovementService.findPendingChanges());
    }

    /**
     * Vueltos pendientes de una persona específica.
     */
    @GetMapping("/pendientes/vueltos/persona/{personId}")
    public ResponseEntity<List<PendingChangeViewResponse>> findPendingChangesByPerson(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer personId) {
        return ResponseEntity.ok(accountMovementService.findPendingChangesByPerson(personId));
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
