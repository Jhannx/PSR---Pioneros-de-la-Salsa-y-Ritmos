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
}
