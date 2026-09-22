package com.jhanp.psraerobicsbackend.modules.person.view;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Getter
@Entity
@Immutable
@Table(name = "vista_saldo_persona")
public class PersonBalanceView {

    @Id
    @Column(name = "persona_id")
    private Integer personId;

    @Column(name = "nombre")
    private String firstName;

    @Column(name = "apellidos")
    private String lastName;

    @Column(name = "total_abonado")
    private BigDecimal totalCredited;

    @Column(name = "total_cobrado")
    private BigDecimal totalCharged;

    @Column(name = "saldo")
    private BigDecimal balance;

    /**
     * Cuánto se le puede devolver en efectivo, que no es lo mismo que el saldo: aquí no se resta lo
     * que ya consumió en asistencias. Es la cifra contra la que valida el endpoint de reembolso.
     */
    @Column(name = "reembolsable")
    private BigDecimal refundable;

    /** Lo que la persona entrego de mas y nunca se le devolvio. Dinero suyo que sigue en la caja. */
    @Column(name = "vuelto_pendiente")
    private BigDecimal pendingChange;

    /** DEBE, VUELTO_PENDIENTE o AL_DIA, resuelto por fn_estado_cuenta. */
    @Column(name = "estado_cuenta")
    private String accountStatus;
}
