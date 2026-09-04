package com.jhanp.psraerobicsbackend.modules.aerobics.report.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.DailyRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.dto.response.SessionRevenueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.mapper.RevenueViewMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.repository.DailyRevenueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.repository.SessionRevenueViewRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.report.service.interfaces.IReportService;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Solo lectura sobre las dos vistas de recaudo. El módulo no tiene entidad de tabla propia.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService implements IReportService {

    private final DailyRevenueViewRepository dailyRevenueViewRepository;
    private final SessionRevenueViewRepository sessionRevenueViewRepository;
    private final RevenueViewMapper revenueViewMapper;

    @Override
    public List<DailyRevenueViewResponse> getDailyRevenue(LocalDate date) {
        log.info("Consultando el recaudo del día: {}", date);
        return dailyRevenueViewRepository.findByDateOrderBySessionIdAsc(date).stream()
                .map(revenueViewMapper::toResponse)
                .toList();
    }

    @Override
    public List<DailyRevenueViewResponse> getDailyRevenueBetween(LocalDate from, LocalDate to) {
        log.info("Consultando el recaudo diario entre {} y {}", from, to);
        validateDateRange(from, to);
        return dailyRevenueViewRepository.findByDateBetweenOrderByDateDesc(from, to).stream()
                .map(revenueViewMapper::toResponse)
                .toList();
    }

    @Override
    public Optional<SessionRevenueViewResponse> getSessionRevenue(Integer sessionId) {
        log.info("Consultando el recaudo de la sesión ID: {}", sessionId);
        return sessionRevenueViewRepository.findBySessionId(sessionId)
                .map(revenueViewMapper::toResponse);
    }

    @Override
    public List<SessionRevenueViewResponse> getSessionRevenueBetween(LocalDate from, LocalDate to) {
        log.info("Consultando el recaudo por sesión entre {} y {}", from, to);
        validateDateRange(from, to);
        return sessionRevenueViewRepository.findByDateBetweenOrderByDateDesc(from, to).stream()
                .map(revenueViewMapper::toResponse)
                .toList();
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            log.warn("Rango de fechas inválido: fecha inicio ({}) es posterior a fecha fin ({})", from, to);
            throw new InvalidDataException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}
