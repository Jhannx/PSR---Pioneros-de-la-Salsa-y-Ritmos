package com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementPatchRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.ApplyChangeRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PersonRefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.RefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.AccountMovementResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PendingChangeViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PersonRefundResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.AccountMovement;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.entity.PaymentMethod;
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
        requireActivePaymentMethod(request.paymentMethodId());

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
        requireActivePaymentMethod(request.paymentMethodId());

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

        requireActivePaymentMethod(request.paymentMethodId());

        List<AccountMovementRepository.RefundableMovement> refundable =
                accountMovementRepository.findRefundableByPerson(personId);

        // El tope sale de fn_saldo_reembolsable y NO de sumar los abonos sin devolver. La diferencia
        // es que la función acota al saldo a favor: sumando los abonos, quien abonó 15.000 y ya
        // consumió 10.000 en clases pasaba la validación por 15.000 y la cuenta terminaba debiendo.
        // Es la misma función que alimenta el campo `refundable` de la API, así que lo que el
        // formulario ofrece y lo que el servidor acepta no pueden divergir.
        BigDecimal available = accountMovementRepository.findRefundableTotal(personId);

        if (available == null) {
            available = BigDecimal.ZERO;
        }

        if (available.compareTo(request.amount()) < 0) {
            throw new InvalidDataException(
                    "No se puede reembolsar " + request.amount() + ": la persona solo tiene "
                            + available + " disponible para devolver.");
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
            requireActivePaymentMethod(request.paymentMethodId());
            movement.setPaymentMethod(paymentMethodRepository.getReferenceById(request.paymentMethodId()));
        }
        if (request.note() != null) movement.setNote(request.note());

        AccountMovement saved = accountMovementRepository.save(movement);
        log.info("Movimiento corregido: ID {} por usuario ID {}", id, executedByUserId);
        return accountMovementMapper.toResponse(saved);
    }

    /**
     * Convierte en abono el vuelto que quedo pendiente de un cobro, sin que entre dinero nuevo.
     * <p>
     * <b>El caso real:</b> alguien paga una clase de 4.000 con un billete de 50.000 y no hay con que
     * dar el cambio. Se registra el cobro, quedan 46.000 pendientes de devolver, y mas tarde la
     * persona prefiere dejarlos como dias pagados por adelantado en vez de llevarse el efectivo.
     * <p>
     * <b>Por que sube monto_abonado del movimiento original y no crea uno nuevo.</b> Crear un abono
     * aparte contaria el dinero dos veces: monto_recibido volveria a sumar unos billetes que ya
     * entraron, y el recaudo del dia saldria inflado. Aqui no entra plata — se reasigna la que ya
     * estaba: lo que era "pendiente de devolver" pasa a ser "credito para clases". La suma de
     * recibido no se toca.
     * <p>
     * Si se abona el vuelto entero, el movimiento deja de tener vuelto (recibido = abonado) y sale
     * solo de vista_vueltos_pendientes. Si se abona una parte, el resto sigue pendiente.
     * <p>
     * Pasa por auditoria como cualquier correccion de movimiento: es un UPDATE sobre
     * movimiento_cuenta, y el trigger exige saber quien lo hizo.
     */
    @Override
    @Transactional
    public AccountMovementResponse applyPendingChange(Integer id, ApplyChangeRequest request,
                                                      Integer executedByUserId) {

        AccountMovement movement = accountMovementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el movimiento con ID: " + id));

        if (movement.getOriginalMovement() != null) {
            throw new InvalidDataException(
                    "Un reembolso no tiene vuelto que abonar: elige el cobro original.");
        }

        if (Boolean.TRUE.equals(movement.getChangeDelivered())) {
            throw new InvalidDataException(
                    "El vuelto de ese cobro ya se entregó, no queda nada que abonar.");
        }

        BigDecimal pending = movement.getAmountReceived().subtract(movement.getAmountCredited());

        if (pending.signum() <= 0) {
            throw new InvalidDataException("Ese cobro no tiene vuelto pendiente.");
        }

        if (request.amount().compareTo(pending) > 0) {
            throw new InvalidDataException(
                    "No se puede abonar " + request.amount() + ": el vuelto pendiente de ese cobro es "
                            + pending + ".");
        }

        auditContextRepository.setCurrentUser(executedByUserId);

        movement.setAmountCredited(movement.getAmountCredited().add(request.amount()));

        if (request.note() != null && !request.note().isBlank()) {
            movement.setNote(request.note());
        }

        // Abonado el vuelto entero, ya no queda nada que devolver: se marca entregado para que el
        // movimiento salga de la lista de vueltos pendientes. "Entregado" aqui significa resuelto, que
        // es lo que esa bandera representa — el dinero dejo de estar esperando a que alguien decida.
        if (movement.getAmountReceived().compareTo(movement.getAmountCredited()) == 0) {
            movement.setChangeDelivered(true);
        }

        AccountMovement saved = accountMovementRepository.save(movement);

        log.info("Vuelto abonado: movimiento {}, monto {}, por usuario {}",
                id, request.amount(), executedByUserId);

        // Cambia el saldo y el estado de cuenta de la persona, asi que la fila tiene que enterarse.
        eventPublisher.publishEvent(QueueChangedEvent.payment(null));

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

    /**
     * Ningún movimiento de dinero puede quedar registrado con un método de pago que administración ya
     * dio de baja.
     * <p>
     * Se comprueba en el servidor y no solo llenando el selector con los activos: ese filtro es de
     * interfaz y se salta con una llamada directa a la API. Y un método se desactiva justamente cuando
     * deja de usarse — una caja que ya no existe, una cuenta cerrada—, así que seguir aceptando cobros
     * con él produce asientos que después no se pueden cuadrar contra nada.
     * <p>
     * null pasa sin comprobar: quién exige el método y quién no lo deciden las validaciones de cada
     * DTO, no este método.
     */
    private void requireActivePaymentMethod(Integer paymentMethodId) {
        if (paymentMethodId == null) {
            return;
        }

        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el método de pago con ID: " + paymentMethodId));

        if (!Boolean.TRUE.equals(method.getActive())) {
            throw new InvalidDataException(
                    "El método de pago '" + method.getName() + "' está desactivado y no se puede usar "
                            + "para registrar dinero. Elige uno de los activos.");
        }
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
