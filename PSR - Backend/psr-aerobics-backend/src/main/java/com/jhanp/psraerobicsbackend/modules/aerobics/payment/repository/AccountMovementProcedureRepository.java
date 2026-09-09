package com.jhanp.psraerobicsbackend.modules.aerobics.payment.repository;

import com.jhanp.psraerobicsbackend.shared.persistence.AuditContextRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/**
 * Acceso a los procedimientos de movimientos de cuenta. Abonos y reembolsos no se replican con JPA:
 * el procedimiento valida la persona activa y el tope contra el monto original.
 */
@Repository
public class AccountMovementProcedureRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AuditContextRepository auditContextRepository;

    public AccountMovementProcedureRepository(JdbcTemplate jdbcTemplate,
                                              AuditContextRepository auditContextRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditContextRepository = auditContextRepository;
    }

    /**
     * sp_registrar_movimiento. checkinId puede ser null (pago por adelantado, antes de llegar).
     * Es un INSERT, así que no dispara el trigger de auditoría y no necesita app.usuario_id.
     * Puede propagar SQLSTATE 75001 si la persona está desactivada.
     */
    public void registerDeposit(Integer personId,
                                Integer checkinId,
                                BigDecimal amountReceived,
                                BigDecimal amountCredited,
                                Integer registeredByUserId,
                                Integer paymentMethodId,
                                String note) {
        jdbcTemplate.update("CALL sp_registrar_movimiento(?, ?, ?, ?, ?, ?, ?)",
                personId, checkinId, amountReceived, amountCredited,
                registeredByUserId, paymentMethodId, note);
    }

    /**
     * sp_registrar_reembolso. El monto se registra en negativo dentro del procedimiento y no puede
     * superar el abono original.
     */
    public void registerRefund(Integer originalMovementId,
                               BigDecimal amount,
                               Integer registeredByUserId,
                               Integer paymentMethodId,
                               String note) {
        jdbcTemplate.update("CALL sp_registrar_reembolso(?, ?, ?, ?, ?)",
                originalMovementId, amount, registeredByUserId, paymentMethodId, note);
    }

    /**
     * sp_entregar_vuelto. Hace UPDATE sobre movimiento_cuenta, y fn_trg_auditar_movimiento_cuenta
     * llama a fn_usuario_actual() de forma incondicional, así que app.usuario_id debe estar puesto
     * en esta misma transacción o el UPDATE falla.
     */
    public void deliverChange(Integer movementId, Integer executedByUserId) {
        auditContextRepository.setCurrentUser(executedByUserId);
        jdbcTemplate.update("CALL sp_entregar_vuelto(?)", movementId);
    }
}
