package com.jhanp.psraerobicsbackend.modules.aerobics.payment.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PaymentMethodRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PaymentMethodResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces.IPaymentMethodService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.jhanp.psraerobicsbackend.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de métodos de pago (Efectivo, Nequi, Daviplata, etc.).
 * <p>
 * Los métodos de pago nunca se eliminan físicamente (hay movimientos que los referencian);
 * para darlos de baja se actualizan con active = false.
 */
@RestController
@RequestMapping("/api/metodos-pago")
// El mostrador los lista para registrar un abono; el catálogo en sí lo mantiene administración.
@PreAuthorize(Authorities.DESK)
@RequiredArgsConstructor
@Validated
public class PaymentMethodController {

    private final IPaymentMethodService paymentMethodService;

    /**
     * Lista todos los métodos de pago o solo los activos según el parámetro ?activo=.
     * <p>
     * Por defecto (?activo no especificado o false) lista todos los métodos; ?activo=true devuelve solo los activos.
     */
    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> findAll(
            @RequestParam(required = false) Boolean activo) {

        if (Boolean.TRUE.equals(activo)) {
            return ResponseEntity.ok(paymentMethodService.findAllActive());
        }
        return ResponseEntity.ok(paymentMethodService.findAll());
    }

    /**
     * Consulta un método de pago por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> findById(
            @PathVariable @Positive(message = "El ID del método de pago debe ser positivo") Integer id) {

        return ResponseEntity.ok(paymentMethodService.findById(id));
    }

    /**
     * Registra un nuevo método de pago.
     */
    @PostMapping
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<PaymentMethodResponse> create(
            @Valid @RequestBody PaymentMethodRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMethodService.create(request));
    }

    /**
     * Actualiza el nombre o estado activo de un método de pago existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<PaymentMethodResponse> update(
            @PathVariable @Positive(message = "El ID del método de pago debe ser positivo") Integer id,
            @Valid @RequestBody PaymentMethodRequest request) {

        return ResponseEntity.ok(paymentMethodService.update(id, request));
    }

    /**
     * Vuelve a poner el método disponible para cobrar.
     * <p>
     * Sin cuerpo, mismo patrón que {@code PATCH /api/usuarios/{id}/activar} y el de horarios. No se
     * hace con el PUT porque ese exige el nombre, y reenviarlo para tocar solo el estado pisa
     * cualquier renombrado que haya ocurrido mientras tanto.
     */
    @PatchMapping("/{id}/activar")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<PaymentMethodResponse> activate(
            @PathVariable @Positive(message = "El ID del método de pago debe ser positivo") Integer id) {

        return ResponseEntity.ok(paymentMethodService.setActive(id, true));
    }

    /**
     * Retira el método del selector de cobro. No lo borra: hay movimientos que lo referencian y
     * borrarlo dejaría el histórico contable sin método.
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<PaymentMethodResponse> deactivate(
            @PathVariable @Positive(message = "El ID del método de pago debe ser positivo") Integer id) {

        return ResponseEntity.ok(paymentMethodService.setActive(id, false));
    }
}
