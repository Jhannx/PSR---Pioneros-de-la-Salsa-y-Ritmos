package com.jhanp.psraerobicsbackend.modules.aerobics.report.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * vista_recaudo_por_sesion: recaudo de cada sesión, para los días con más de una. Solo cuenta
 * movimientos ligados a una llegada.
 * <p>
 * Misma forma que {@link DailyRevenueView}: el contrato JSON es idéntico, cambia la agregación.
 */
@Getter
@Entity
@Immutable
@Table(name = "vista_recaudo_por_sesion")
public class SessionRevenueView {

    @Id
    @Column(name = "sesion_id")
    private Integer sessionId;

    @Column(name = "fecha")
    private LocalDate date;

    @Column(name = "total_recaudado")
    private BigDecimal totalCollected;
}
