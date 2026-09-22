package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.view.AttendanceBalanceView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Solo lectura sobre vista_saldo_por_asistencia: para cada asistencia cobrada dice si el saldo
 * acumulado la cubría. balanceStatus toma los valores CUBIERTA, PARCIAL o DEBE.
 * <p>
 * La consulta general va paginada porque hay una fila por asistencia y eso solo crece. El filtro
 * por estado no: sirve para "¿qué debe esta persona?", que es una lista corta y se quiere completa.
 */
public interface AttendanceBalanceViewRepository extends Repository<AttendanceBalanceView, Integer> {

    Page<AttendanceBalanceView> findByPersonIdOrderByDateAsc(Integer personId, Pageable pageable);

    List<AttendanceBalanceView> findByPersonIdAndBalanceStatusOrderByDateAsc(Integer personId,
                                                                             String balanceStatus);
}
