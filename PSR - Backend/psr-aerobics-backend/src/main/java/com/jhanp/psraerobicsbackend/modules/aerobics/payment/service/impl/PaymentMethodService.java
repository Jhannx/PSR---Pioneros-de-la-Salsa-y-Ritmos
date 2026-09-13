package com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PaymentMethodRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PaymentMethodResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.PaymentMethod;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper.PaymentMethodMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.PaymentMethodRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces.IPaymentMethodService;
import com.jhanp.psraerobicsbackend.shared.exception.DuplicateResourceException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentMethodService implements IPaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMethodMapper paymentMethodMapper;

    @Override
    public List<PaymentMethodResponse> findAll() {
        return paymentMethodRepository.findAll().stream()
                .map(paymentMethodMapper::toResponse)
                .toList();
    }

    @Override
    public List<PaymentMethodResponse> findAllActive() {
        return paymentMethodRepository.findByActiveTrue().stream()
                .map(paymentMethodMapper::toResponse)
                .toList();
    }

    @Override
    public PaymentMethodResponse findById(Integer id) {
        return paymentMethodMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        if (paymentMethodRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe un método de pago con el nombre: " + request.name());
        }

        PaymentMethod saved = paymentMethodRepository.save(paymentMethodMapper.toEntity(request));
        log.info("Método de pago creado con ID: {}", saved.getId());
        return paymentMethodMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentMethodResponse update(Integer id, PaymentMethodRequest request) {
        PaymentMethod paymentMethod = findEntity(id);

        if (!paymentMethod.getName().equalsIgnoreCase(request.name())
                && paymentMethodRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Ya existe un método de pago con el nombre: " + request.name());
        }

        paymentMethod.setName(request.name());
        // null = no se toca el estado; apagarlo es mandar active = false explícitamente.
        if (request.active() != null) {
            paymentMethod.setActive(request.active());
        }

        log.info("Método de pago ID: {} actualizado", id);
        return paymentMethodMapper.toResponse(paymentMethodRepository.save(paymentMethod));
    }

    @Override
    @Transactional
    public PaymentMethodResponse setActive(Integer id, boolean active) {
        PaymentMethod paymentMethod = findEntity(id);
        paymentMethod.setActive(active);

        log.info("Método de pago ID: {} {}", id, active ? "activado" : "desactivado");
        return paymentMethodMapper.toResponse(paymentMethodRepository.save(paymentMethod));
    }

    private PaymentMethod findEntity(Integer id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago no encontrado con ID: " + id));
    }
}
