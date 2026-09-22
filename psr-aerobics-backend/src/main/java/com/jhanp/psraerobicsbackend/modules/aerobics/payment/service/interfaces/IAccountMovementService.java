package com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementPatchRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.AccountMovementRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.ApplyChangeRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PersonRefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.RefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.AccountMovementResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PendingChangeViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PersonRefundResponse;
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
     * Devuelve un monto a una persona sin que quien llama tenga que decidir contra qué abonos va.
     * <p>
     * Reparte FIFO —del abono más antiguo al más reciente— entre los que aún tienen saldo por
     * devolver, y lo hace en UNA transacción: o se aplica entero o no se aplica nada. Si el total
     * disponible no alcanza, falla con 400 antes de emitir ningún movimiento.
     * <p>
     * Existe porque el reparto es regla de negocio y estaba viviendo en el frontend, encadenando una
     * llamada a {@link #registerRefund} por abono. Aquello funcionaba, pero cada llamada era su propia
     * transacción y un fallo a mitad dejaba dinero devuelto a medias.
     */
    PersonRefundResponse refundByPerson(Integer personId,
                                        PersonRefundRequest request,
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

    /**
     * Convierte en abono el vuelto pendiente de un cobro, total o parcialmente.
     * <p>
     * <b>No entra dinero nuevo.</b> Sube el monto abonado del mismo movimiento en vez de crear otro:
     * los billetes ya se recibieron y ya constan: lo unico que cambia es que dejan de estar
     * pendientes de devolver y pasan a ser credito para clases. Crear un abono aparte contaria dos
     * veces el mismo dinero e inflaria el recaudo del dia.
     * <p>
     * 400 si el movimiento es un reembolso, si su vuelto ya se entrego, si no tiene vuelto pendiente o
     * si se pide mas de lo que queda.
     */
    AccountMovementResponse applyPendingChange(Integer id,
                                               ApplyChangeRequest request,
                                               Integer executedByUserId);

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
