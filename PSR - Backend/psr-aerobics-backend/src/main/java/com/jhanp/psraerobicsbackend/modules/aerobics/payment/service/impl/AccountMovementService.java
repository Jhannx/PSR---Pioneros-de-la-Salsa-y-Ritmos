package com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementPatchRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PersonRefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.RefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.AccountMovementResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PendingChangeViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PersonRefundResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.AccountMovement;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper.AccountMovementMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.mapper.PendingChangeViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.AccountMovementProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.AccountMovementRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.PaymentMethodRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository.PendingChangeViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces.IAccountMovementService;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.event.QueueChangedEvent;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.persistence.AuditContextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private final ApplicationEventPublisher eventPublisher;

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

        // Un abono mueve el saldo del panel y puede hacer aparecer a la persona en la fila de
        // prepagados. Va sin sesión: un pago por adelantado no cuelga de ninguna llegada.
        eventPublisher.publishEvent(QueueChangedEvent.payment(null));

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

        // Patrón H: El reembolso es el último movimiento relacionado con el original. Lo resuelve la
        // consulta, no un filtro en memoria sobre findAll(): la tabla de movimientos crece sin techo.
        AccountMovement refund = accountMovementRepository
                .findTopByOriginalMovementIdOrderByTimestampDesc(originalMovementId)
                .orElseThrow(() -> new IllegalStateException("El reembolso no se registró correctamente."));

        log.info("Reembolso registrado: ID {}, Original ID {}, Monto {}", refund.getId(), originalMovementId, request.amount());
        return accountMovementMapper.toResponse(refund);
    }

    /**
     * Reparte un monto entre los abonos de la persona que aún tienen saldo por devolver.
     * <p>
     * FIFO: el abono más antiguo primero. Es una decisión de negocio y por eso vive aquí y no en el
     * frontend — determina contra qué movimiento queda registrada cada devolución, y se elige FIFO
     * porque es el criterio que quien atiende puede explicar en voz alta.
     * <p>
     * <b>Todo o nada.</b> El @Transactional cubre las N llamadas al procedimiento, así que si una
     * falla se deshacen las anteriores. Eso es lo que arregla el problema real del encadenado desde el
     * frontend: allí cada llamada era su propia transacción y un fallo a mitad dejaba dinero devuelto
     * a medias, sin forma de saber cuánto.
     * <p>
     * Se comprueba el total disponible ANTES de emitir nada. Sin esa comprobación previa el reparto
     * iría creando reembolsos hasta quedarse corto y solo entonces fallaría: correcto por la
     * transacción, pero desperdicia el trabajo y el mensaje de error llegaría desde el procedimiento,
     * hablando de un abono suelto en vez de del total que falta.
     */
    @Override
    @Transactional
    public PersonRefundResponse refundByPerson(Integer personId, PersonRefundRequest request,
                                               Integer registeredByUserId) {

        List<AccountMovementRepository.RefundableMovement> refundable =
                accountMovementRepository.findRefundableByPerson(personId);

        BigDecimal available = refundable.stream()
                .map(AccountMovementRepository.RefundableMovement::getAvailable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (available.compareTo(request.amount()) < 0) {
            throw new InvalidDataException(
                    "No se puede reembolsar " + request.amount() + ": la persona solo tiene "
                            + available + " pendiente de devolver.");
        }

        List<PersonRefundResponse.Allocation> allocations = new ArrayList<>();
        BigDecimal pending = request.amount();

        for (AccountMovementRepository.RefundableMovement movement : refundable) {
            if (pending.signum() == 0) {
                break;
            }

            // De este abono sale lo que quede por repartir, o todo su saldo si no alcanza.
            BigDecimal take = pending.min(movement.getAvailable());

            accountMovementProcedureRepository.registerRefund(
                    movement.getMovementId(), take, registeredByUserId,
                    request.paymentMethodId(), request.note());

            // Patrón H, igual que en registerRefund: el procedimiento no devuelve el id.
            AccountMovement refund = accountMovementRepository
                    .findTopByOriginalMovementIdOrderByTimestampDesc(movement.getMovementId())
                    .orElseThrow(() -> new IllegalStateException(
                            "El reembolso contra el movimiento " + movement.getMovementId()
                                    + " no se registró correctamente."));

            allocations.add(new PersonRefundResponse.Allocation(
                    movement.getMovementId(), refund.getId(), take,
                    movement.getAvailable().subtract(take)));

            pending = pending.subtract(take);
        }

        log.info("Reembolso por persona: Persona {}, Monto {}, repartido entre {} abono(s)",
                personId, request.amount(), allocations.size());

        // Mueve el saldo del panel igual que un abono, así que la fila tiene que enterarse.
        eventPublisher.publishEvent(QueueChangedEvent.payment(null));

        return new PersonRefundResponse(personId, request.amount(), allocations);
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
