package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleExceptionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleExceptionResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.ScheduleException;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.mapper.ScheduleExceptionMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository.ScheduleExceptionRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository.ScheduleRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.interfaces.IScheduleExceptionService;
import com.jhanp.psraerobicsbackend.shared.exception.DuplicateResourceException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.validation.DateRanges;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScheduleExceptionService implements IScheduleExceptionService {

    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleExceptionMapper scheduleExceptionMapper;

    @Override
    @Transactional
    public ScheduleExceptionResponse create(Integer scheduleId, ScheduleExceptionRequest request) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Horario no encontrado con ID: " + scheduleId);
        }
        if (scheduleExceptionRepository.existsByScheduleIdAndDate(scheduleId, request.date())) {
            throw new DuplicateResourceException(
                    "Ya existe una excepción registrada para este horario en la fecha " + request.date());
        }

        ScheduleException exception = scheduleExceptionMapper.toEntity(request);
        exception.setSchedule(scheduleRepository.getReferenceById(scheduleId));

        ScheduleException saved = scheduleExceptionRepository.save(exception);
        log.info("Excepción creada para el horario ID: {} en la fecha: {}", scheduleId, request.date());
        return scheduleExceptionMapper.toResponse(saved);
    }

    @Override
    public ScheduleExceptionResponse findById(Integer id) {
        return scheduleExceptionMapper.toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        scheduleExceptionRepository.delete(findEntity(id));
        log.info("Excepción de horario ID: {} eliminada", id);
    }

    @Override
    public List<ScheduleExceptionResponse> findBySchedule(Integer scheduleId) {
        return scheduleExceptionRepository.findByScheduleIdOrderByDateAsc(scheduleId).stream()
                .map(scheduleExceptionMapper::toResponse)
                .toList();
    }

    @Override
    public List<ScheduleExceptionResponse> findBetween(LocalDate from, LocalDate to) {
        DateRanges.require(from, to);
        return scheduleExceptionRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(scheduleExceptionMapper::toResponse)
                .toList();
    }

    @Override
    public boolean exists(Integer scheduleId, LocalDate date) {
        return scheduleExceptionRepository.existsByScheduleIdAndDate(scheduleId, date);
    }

    private ScheduleException findEntity(Integer id) {
        return scheduleExceptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Excepción de horario no encontrada con ID: " + id));
    }
}
