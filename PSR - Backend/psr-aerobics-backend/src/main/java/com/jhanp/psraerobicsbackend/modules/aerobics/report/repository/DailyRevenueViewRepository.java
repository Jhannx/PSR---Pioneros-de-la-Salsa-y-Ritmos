package com.jhanp.psraerobicsbackend.modules.aerobics.report.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.view.DailyRevenueView;
import org.springframework.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Solo lectura sobre vista_recaudo_diario: total recaudado por día y sesión, incluyendo los
 * movimientos sin llegada asociada (pagos por adelantado), que aparecen con sessionId nulo.
 * <p>
 * El id es una clave sintética que la vista genera con ROW_NUMBER(): identifica la fila dentro del
 * resultado, no es estable entre consultas y no debe exponerse en la API.
 */
public interface DailyRevenueViewRepository extends Repository<DailyRevenueView, Long> {

    List<DailyRevenueView> findByDateOrderBySessionIdAsc(LocalDate date);

    List<DailyRevenueView> findByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);
}
