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
 * vista_recaudo_diario: total recaudado por día y sesión. Los pagos por adelantado, que no cuelgan
 * de ninguna llegada, aparecen con sessionId nulo.
 * <p>
 * Misma forma que {@link SessionRevenueView} — son dos agregaciones distintas en SQL sobre un mismo
 * contrato JSON, no dos contratos.
 */
@Getter
@Entity
@Immutable
@Table(name = "vista_recaudo_diario")
public class DailyRevenueView {

    // Clave sintética generada por la vista con ROW_NUMBER(): no existe en ninguna tabla, no es
    // estable entre consultas y solo sirve para que Hibernate pueda identificar cada fila.
    // DailyRevenueViewResponse no la declara, así que nunca sale a la API.
    @Id
    private Long id;

    @Column(name = "fecha")
    private LocalDate date;

    @Column(name = "sesion_id")
    private Integer sessionId;

    @Column(name = "total_recaudado")
    private BigDecimal totalCollected;
}
