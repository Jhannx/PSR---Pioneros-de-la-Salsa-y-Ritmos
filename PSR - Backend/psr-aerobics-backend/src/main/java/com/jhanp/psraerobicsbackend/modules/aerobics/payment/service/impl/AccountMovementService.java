package com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementPatchRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.RefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.AccountMovementResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PendingChangeViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.AccountMovement;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper.AccountMovementMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper.PendingChangeViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.AccountMovementProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.AccountMovementRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.PaymentMethodRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.PendingChangeViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces.IAccountMovementService;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.persistence.AuditContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountMovementService implements IAccountMovementService {

    private final AccountMovementRepository accountMovementRepository;
    private final AccountMovementProcedureRepository accountMovementProcedureRepository;
    private final PendingChangeViewRepository pendingChangeViewRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final AuditContextRepository auditContextRepository;
    private final AccountMovementMapper accountMovementMapper;
    private final PendingChangeViewMapper pendingChangeViewMapper;

    @Override
    @Transactional
    public AccountMovementResponse registerDeposit(AccountMovementRequest request, Integer registeredByUserId) {
        accountMovementProcedureRepository.registerDeposit(
                request.personId(),
                request.checkinId(),
                request.amountReceived(),
                request.amountApplied(),
                registeredByUserId,
                request.paymentMethodId(),
                request.note()
        );

        // Patrón H: Releer el último movimiento de la persona
        AccountMovement movement = accountMovementRepository.findByPersonIdOrderByTimestampDesc(request.personId(), Pageable.ofSize(1))
                .getContent().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El abono no se registró correctamente."));

        log.info("Abono registrado: ID {}, Persona {}, Monto {}", movement.getId(), request.personId(), request.amountApplied());
        return accountMovementMapper.toResponse(movement);
    }

    @Override
    @Transactional
    public AccountMovementResponse registerRefund(Integer originalMovementId, RefundRequest request, Integer registeredByUserId) {
        accountMovementProcedureRepository.registerRefund(
                originalMovementId,
                request.amount(),
                registeredByUserId,
                request.paymentMethodId(),
                request.note()
        );

        // Patrón H: El reembolso es el último movimiento relacionado con el original
        AccountMovement refund = accountMovementRepository.findAll().stream()
                .filter(m -> originalMovementId.equals(m.getOriginalMovement() != null ? m.getOriginalMovement().getId() : null))
                .max(Comparator.comparing(AccountMovement::getTimestamp))
                .orElseThrow(() -> new IllegalStateException("El reembolso no se registró correctamente."));

        log.info("Reembolso registrado: ID {}, Original ID {}, Monto {}", refund.getId(), originalMovementId, request.amount());
        return accountMovementMapper.toResponse(refund);
    }

    @Override
    @Transactional
    public AccountMovementResponse correct(Integer id, AccountMovementPatchRequest request, Integer executedByUserId) {
        AccountMovement movement = accountMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el movimiento con ID: " + id));

        auditContextRepository.setCurrentUser(executedByUserId);

        if (request.amountReceived() != null) movement.setAmountReceived(request.amountReceived());
        if (request.amountApplied() != null) movement.setAmountCredited(request.amountApplied());
        if (request.paymentMethodId() != null) {
            movement.setPaymentMethod(paymentMethodRepository.getReferenceById(request.paymentMethodId()));
        }
        if (request.note() != null) movement.setNote(request.note());

        AccountMovement saved = accountMovementRepository.save(movement);
        log.info("Movimiento corregido: ID {} por usuario ID {}", id, executedByUserId);
        return accountMovementMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AccountMovementResponse deliverChange(Integer id, Integer executedByUserId) {
        accountMovementProcedureRepository.deliverChange(id, executedByUserId);

        AccountMovement movement = accountMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el movimiento con ID: " + id));

        log.info("Vuelto entregado: ID {} por usuario ID {}", id, executedByUserId);
        return accountMovementMapper.toResponse(movement);
    }

    @Override
    public AccountMovementResponse findById(Integer id) {
        return accountMovementRepository.findById(id)
                .map(accountMovementMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el movimiento con ID: " + id));
    }

    @Override
    public Page<AccountMovementResponse> findByPerson(Integer personId, Pageable pageable) {
        return accountMovementRepository.findByPersonIdOrderByTimestampDesc(personId, pageable)
                .map(accountMovementMapper::toResponse);
    }

    @Override
    public List<AccountMovementResponse> findByCheckin(Integer checkinId) {
        return accountMovementRepository.findByCheckinId(checkinId).stream()
                .map(accountMovementMapper::toResponse)
                .toList();
    }

    @Override
    public List<PendingChangeViewResponse> findPendingChanges() {
        return pendingChangeViewRepository.findAllByOrderByDateTimeDesc().stream()
                .map(pendingChangeViewMapper::toResponse)
                .toList();
    }

    @Override
    public List<PendingChangeViewResponse> findPendingChangesByPerson(Integer personId) {
        return pendingChangeViewRepository.findByPersonIdOrderByDateTimeDesc(personId).stream()
                .map(pendingChangeViewMapper::toResponse)
                .toList();
    }
}
