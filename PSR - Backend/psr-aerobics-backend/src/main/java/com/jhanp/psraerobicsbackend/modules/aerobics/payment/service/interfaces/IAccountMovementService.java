package com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementPatchRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.RefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.AccountMovementResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PendingChangeViewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IAccountMovementService {

    /**
     * Abono vía sp_registrar_movimiento. El checkinId del request puede ser null (pago adelantado).
     * <p>
     * El procedimiento no devuelve nada: hay que releer movimiento_cuenta para completar id,
     * fecha_hora y el nombre del método de pago (patrón H). Puede propagar SQLSTATE 75001 si la
     * persona está desactivada.
     */
    AccountMovementResponse registerDeposit(AccountMovementRequest request, Integer registeredByUserId);

    /**
     * Reembolso vía sp_registrar_reembolso. El movimiento original va en el path y aporta persona y
     * llegada; el usuario sale del JWT. También exige releer la fila creada para responder.
     */
    AccountMovementResponse registerRefund(Integer originalMovementId,
                                           RefundRequest request,
                                           Integer registeredByUserId);

    /**
     * Corrección de un movimiento ya registrado (monto o método mal digitado), vía JPA. Solo aplica
     * los campos que vengan en el patch.
     * <p>
     * Requiere fijar app.usuario_id en la misma transacción: el UPDATE queda auditado por trigger.
     */
    AccountMovementResponse correct(Integer id, AccountMovementPatchRequest request, Integer executedByUserId);

    /**
     * Marca el vuelto como entregado vía sp_entregar_vuelto. También exige app.usuario_id.
     */
    AccountMovementResponse deliverChange(Integer id, Integer executedByUserId);

    AccountMovementResponse findById(Integer id);

    Page<AccountMovementResponse> findByPerson(Integer personId, Pageable pageable);

    List<AccountMovementResponse> findByCheckin(Integer checkinId);

    /**
     * Vueltos sin entregar, desde vista_vueltos_pendientes. Se saldan con deliverChange. Sin
     * paginar: es una bandeja de pendientes del día, no un histórico.
     */
    List<PendingChangeViewResponse> findPendingChanges();

    List<PendingChangeViewResponse> findPendingChangesByPerson(Integer personId);
}
