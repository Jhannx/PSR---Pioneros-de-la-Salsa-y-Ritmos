package com.jhanp.psraerobicsbackend.modules.aerobics.attendance.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.dto.response.AttendanceResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.entity.Attendance;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.mapper.AttendanceMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.mapper.AttendanceViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository.AttendanceBalanceViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository.AttendanceHistoryViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository.AttendanceProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.repository.AttendanceRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.attendance.service.interfaces.IAttendanceService;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.event.QueueChangedEvent;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.validation.DateRanges;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * No valida la llegada, el usuario ni el monto: sp_registrar_asistencia ya lo hace y lanza si algo
 * no cuadra. Duplicar esas comprobaciones en Java solo agrega consultas y dos fuentes de verdad.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceService implements IAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceProcedureRepository attendanceProcedureRepository;
    private final AttendanceHistoryViewRepository attendanceHistoryViewRepository;
    private final AttendanceBalanceViewRepository attendanceBalanceViewRepository;
    private final AttendanceMapper attendanceMapper;
    private final AttendanceViewMapper attendanceViewMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AttendanceResponse confirm(Integer checkinId, Integer markedByUserId) {
        // Sin set_config: el procedimiento inserta, y trg_auditar_asistencia es AFTER UPDATE.
        attendanceProcedureRepository.confirm(checkinId, markedByUserId);

        // Patrón H: el CALL no devuelve nada y monto_cobrado lo puso fn_tarifa_vigente() adentro,
        // así que hay que releer la fila para poder responder.
        Attendance saved = attendanceRepository.findByCheckinId(checkinId)
                .orElseThrow(() -> new IllegalStateException(
                        "La asistencia no quedó registrada para la llegada " + checkinId));

        log.info("Asistencia confirmada para la llegada ID: {} por el usuario ID: {}", checkinId, markedByUserId);

        // sessionId va null a propósito: sacarlo obligaría a resolver dos relaciones LAZY solo para
        // rellenar un dato que el cliente no necesita —recibe la señal y relee la vista que muestra.
        eventPublisher.publishEvent(QueueChangedEvent.attendanceConfirmed(null));

        return attendanceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void cancel(Integer checkinId) {
        attendanceProcedureRepository.cancel(checkinId);
        log.info("Asistencia anulada para la llegada ID: {}", checkinId);

        eventPublisher.publishEvent(QueueChangedEvent.attendanceCancelled(null));
    }

    @Override
    public AttendanceResponse findByCheckinId(Integer checkinId) {
        return attendanceRepository.findByCheckinId(checkinId)
                .map(attendanceMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La llegada " + checkinId + " no tiene asistencia confirmada"));
    }

    @Override
    public List<AttendanceResponse> findBySession(Integer sessionId) {
        return attendanceRepository.findByCheckinSessionId(sessionId).stream()
                .map(attendanceMapper::toResponse)
                .toList();
    }

    @Override
    public Page<AttendanceResponse> findByPerson(Integer personId, Pageable pageable) {
        return attendanceRepository.findByCheckinPersonIdOrderByMarkedAtDesc(personId, pageable)
                .map(attendanceMapper::toResponse);
    }

    @Override
    public boolean isConfirmed(Integer checkinId) {
        return attendanceRepository.existsByCheckinId(checkinId);
    }

    @Override
    public Page<AttendanceHistoryViewResponse> getHistoryByPerson(Integer personId, Pageable pageable) {
        return attendanceHistoryViewRepository.findByPersonIdOrderByDateDesc(personId, pageable)
                .map(attendanceViewMapper::toResponse);
    }

    @Override
    public Page<AttendanceHistoryViewResponse> getHistoryBySession(Integer sessionId, Pageable pageable) {
        return attendanceHistoryViewRepository.findBySessionIdOrderByArrivalTimeAsc(sessionId, pageable)
                .map(attendanceViewMapper::toResponse);
    }

    @Override
    public Page<AttendanceHistoryViewResponse> getHistoryBetween(LocalDate from, LocalDate to, Pageable pageable) {
        DateRanges.require(from, to);
        return attendanceHistoryViewRepository.findByDateBetweenOrderByDateDesc(from, to, pageable)
                .map(attendanceViewMapper::toResponse);
    }

    @Override
    public Page<AttendanceBalanceViewResponse> getBalanceByPerson(Integer personId, Pageable pageable) {
        return attendanceBalanceViewRepository.findByPersonIdOrderByDateAsc(personId, pageable)
                .map(attendanceViewMapper::toResponse);
    }
}
