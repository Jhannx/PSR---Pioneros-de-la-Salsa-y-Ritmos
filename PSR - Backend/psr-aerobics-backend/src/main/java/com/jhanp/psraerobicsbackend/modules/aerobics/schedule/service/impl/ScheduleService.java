package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.impl;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.entity.Schedule;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.mapper.ScheduleMapper;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.repository.ScheduleRepository;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.interfaces.IScheduleService;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScheduleService implements IScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleResponse> findAll() {
        log.info("Consultando todos los horarios ordenados por día y hora de inicio");
        return scheduleRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    public List<ScheduleResponse> findAllActive() {
        log.info("Consultando horarios activos ordenados por día y hora de inicio");
        return scheduleRepository.findByActiveTrueOrderByDayOfWeekAscStartTimeAsc().stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    public ScheduleResponse findById(Integer id) {
        log.info("Buscando horario con ID: {}", id);
        Schedule schedule = findScheduleById(id);
        return scheduleMapper.toResponse(schedule);
    }

    @Override
    public List<ScheduleResponse> findByDayOfWeek(Integer dayOfWeek) {
        log.info("Consultando horarios activos para el día de la semana: {}", dayOfWeek);
        validateDayOfWeek(dayOfWeek);
        return scheduleRepository.findByDayOfWeekAndActiveTrueOrderByStartTimeAsc(dayOfWeek).stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ScheduleResponse create(ScheduleRequest request) {
        log.info("Creando horario para día {} de {} a {}", request.dayOfWeek(), request.startTime(), request.endTime());
        validateScheduleHours(request.startTime(), request.endTime());
        validateDayOfWeek(request.dayOfWeek());

        Schedule schedule = scheduleMapper.toEntity(request);
        schedule.setActive(true);

        Schedule savedSchedule = scheduleRepository.save(schedule);
        ScheduleResponse response = scheduleMapper.toResponse(savedSchedule);

        log.info("Horario creado exitosamente con ID: {}", response.id());
        return response;
    }

    @Override
    @Transactional
    public ScheduleResponse update(Integer id, ScheduleRequest request) {
        log.info("Actualizando horario ID: {} con día {} de {} a {}", id, request.dayOfWeek(), request.startTime(), request.endTime());
        Schedule schedule = findScheduleById(id);
        validateScheduleHours(request.startTime(), request.endTime());
        validateDayOfWeek(request.dayOfWeek());

        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        ScheduleResponse response = scheduleMapper.toResponse(updatedSchedule);

        log.info("Horario ID: {} actualizado exitosamente", response.id());
        return response;
    }

    @Override
    @Transactional
    public ScheduleResponse setActive(Integer id, boolean active) {
        log.info("Cambiando estado de horario ID: {} a activo: {}", id, active);
        Schedule schedule = findScheduleById(id);
        schedule.setActive(active);

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        ScheduleResponse response = scheduleMapper.toResponse(updatedSchedule);

        log.info("Estado de horario ID: {} actualizado a activo: {}", response.id(), active);
        return response;
    }

    private Schedule findScheduleById(Integer id) {
        return scheduleRepository.findById(id).orElseThrow(() -> {
            log.warn("Horario no encontrado con ID: {}", id);
            return new ResourceNotFoundException("Horario no encontrado con ID: " + id);
        });
    }

    private void validateScheduleHours(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            log.warn("Horas de horario inválidas: hora inicio ({}) debe ser anterior a hora fin ({})", startTime, endTime);
            throw new InvalidDataException("La hora de inicio debe ser estrictamente anterior a la hora de fin");
        }
    }

    private void validateDayOfWeek(Integer dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            log.warn("Día de la semana inválido: {}", dayOfWeek);
            throw new InvalidDataException("El día de la semana debe ser un valor entre 1 (lunes) y 7 (domingo)");
        }
    }
}
