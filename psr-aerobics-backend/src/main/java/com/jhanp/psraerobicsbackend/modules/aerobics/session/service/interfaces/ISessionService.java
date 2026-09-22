package com.jhanp.psraerobicsbackend.modules.aerobics.session.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.request.SessionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionAttendeeResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionSummaryViewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ISessionService {

    /**
     * Creación manual por parte del admin, vía sp_crear_sesion_manual. No existe un alta por JPA:
     * la sesión del flujo normal nace desde sp_registrar_llegada.
     * <p>
     * El procedimiento devuelve el id por OUT; el resto del Response sale de releer la fila.
     */
    SessionResponse createManual(SessionRequest request);

    /**
     * Cancelación vía sp_cancelar_sesion: queda auditada en historial_cambio con el usuario del JWT,
     * que el procedimiento recibe por parámetro (patrón G).
     */
    SessionResponse cancel(Integer id, Integer executedByUserId);

    SessionResponse findById(Integer id);

    /**
     * Sesiones de un día concreto: son una o dos, no se pagina.
     */
    List<SessionResponse> findByDate(LocalDate date);

    /**
     * Rango de fechas: paginado. Un mes ya pasa de 10 filas.
     */
    Page<SessionResponse> findBetween(LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Sesión del día todavía en curso, si la hay.
     */
    /**
     * Listado con filtro de estado: {@code cancelled} en null trae las dos, true solo las canceladas,
     * false solo las vigentes. El rango sigue siendo opcional, con la misma regla de "o los dos o
     * ninguno".
     */
    Page<SessionResponse> findBetween(LocalDate from, LocalDate to, Boolean cancelled, Pageable pageable);

    /** Las sesiones asignadas a un profesor, para su propia pantalla. */
    Page<SessionResponse> findByTeacher(Integer teacherUserId, LocalDate from, LocalDate to, Pageable pageable);

    /** Quien llego a una sesion y quien de ellos quedo confirmado, con nombres. */
    List<SessionAttendeeResponse> getAttendees(Integer sessionId);

    /**
     * Si esa sesion esta asignada a ese profesor. Lo usa el control de permisos para dejarle cerrar
     * las suyas y solo las suyas.
     */
    boolean isTeacherOf(Integer sessionId, Integer teacherUserId);

    Optional<SessionResponse> findOpenSession(LocalDate date);

    /**
     * Cierre manual: fija hora_fin. El profesor asignado se cambia con assignTeacher.
     */
    SessionResponse close(Integer id);

    SessionResponse assignTeacher(Integer id, Integer teacherUserId);

    /**
     * Resumen desde vista_resumen_sesiones: profesor resuelto y totales de llegadas y atendidos ya
     * contados.
     */
    Optional<SessionSummaryViewResponse> getSummary(Integer id);

    List<SessionSummaryViewResponse> getSummariesByDate(LocalDate date);

    Page<SessionSummaryViewResponse> getSummariesBetween(LocalDate from, LocalDate to, Pageable pageable);
}
