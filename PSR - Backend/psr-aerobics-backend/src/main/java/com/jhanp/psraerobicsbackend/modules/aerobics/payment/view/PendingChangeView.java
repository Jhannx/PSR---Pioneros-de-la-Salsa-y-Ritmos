package com.jhanp.psraerobicsbackend.modules.aerobics.payment.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * vista_vueltos_pendientes: abonos donde se recibió más de lo abonado y el vuelto todavía no se
 * entregó. Se salda con sp_entregar_vuelto.
 */
@Getter
@Entity
@Immutable
@Table(name = "vista_vueltos_pendientes")
public class PendingChangeView {

    @Id
    @Column(name = "movimiento_id")
    private Integer movementId;

    @Column(name = "persona_id")
    private Integer personId;

    @Column(name = "nombre_completo")
    private String fullName;

    @Column(name = "monto_recibido")
    private BigDecimal amountReceived;

    @Column(name = "monto_abonado")
    private BigDecimal amountCredited;

    @Column(name = "vuelto")
    private BigDecimal change;

    @Column(name = "fecha_hora")
    private OffsetDateTime dateTime;
}
