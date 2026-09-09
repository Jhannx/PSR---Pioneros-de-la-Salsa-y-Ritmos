package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface IAttendanceService {

    /**
     * Confirma la asistencia de una llegada vía sp_registrar_asistencia.
     * <p>
     * No recibe DTO de entrada a propósito: de las cinco columnas de asistencia, ninguna la manda el
     * cliente. id y hora_marcado los pone Postgres, llegada_id viaja en la URL, marcado_por sale del
     * JWT y monto_cobrado lo calcula fn_tarifa_vigente() adentro del procedimiento — ni siquiera es
     * parámetro suyo.
     * <p>
     * Como el CALL no devuelve nada, la implementación relee la fila recién creada para armar el
     * Response (patrón H).
     */
    AttendanceResponse confirm(Integer checkinId, Integer markedByUserId);

    /**
     * Anula la asistencia vía sp_anular_asistencia. Es un DELETE de la fila, no un flag: la llegada
     * no se toca.
     */
    void cancel(Integer checkinId);

    AttendanceResponse findByCheckinId(Integer checkinId);

    List<AttendanceResponse> findBySession(Integer sessionId);

    Page<AttendanceResponse> findByPerson(Integer personId, Pageable pageable);

    boolean isConfirmed(Integer checkinId);

    /**
     * Historial desde vista_historial_asistencias: trae persona, quién marcó y método de pago ya
     * resueltos, sin rehacer los JOIN.
     */
    Page<AttendanceHistoryViewResponse> getHistoryByPerson(Integer personId, Pageable pageable);

    Page<AttendanceHistoryViewResponse> getHistoryBySession(Integer sessionId, Pageable pageable);

    Page<AttendanceHistoryViewResponse> getHistoryBetween(LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Cobertura asistencia por asistencia desde vista_saldo_por_asistencia: dice si el saldo
     * acumulado cubría cada cobro (CUBIERTA, PARCIAL o DEBE).
     * <p>
     * Paginado: hay una fila por asistencia, así que para alguien con dos años de historia son
     * cientos.
     */
    Page<AttendanceBalanceViewResponse> getBalanceByPerson(Integer personId, Pageable pageable);
}
