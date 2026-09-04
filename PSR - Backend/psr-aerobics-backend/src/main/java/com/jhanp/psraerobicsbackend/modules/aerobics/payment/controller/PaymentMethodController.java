package com.jhanp.psraerobicsbackend.modules.aerobics.payment.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PaymentMethodRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PaymentMethodResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces.IPaymentMethodService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<PaymentMethodResponse> create(
            @Valid @RequestBody PaymentMethodRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(paymentMethodService.create(request));
    }

    /**
     * Actualiza el nombre o estado activo de un método de pago existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethodResponse> update(
            @PathVariable @Positive(message = "El ID del método de pago debe ser positivo") Integer id,
            @Valid @RequestBody PaymentMethodRequest request) {

        return ResponseEntity.ok(paymentMethodService.update(id, request));
    }
}
