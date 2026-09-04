package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleExceptionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleExceptionResponse;

import java.time.LocalDate;
import java.util.List;

public interface IScheduleExceptionService {

    /**
     * Declara que un horario no aplica en una fecha. fn_horario_vigente_ahora() la respeta al
     * resolver la sesión del momento. La UNIQUE (horario_id, fecha) impide declararla dos veces.
     * <p>
     * scheduleId va como parámetro porque viaja en el path, no en el body.
     */
    ScheduleExceptionResponse create(Integer scheduleId, ScheduleExceptionRequest request);

    ScheduleExceptionResponse findById(Integer id);

    void delete(Integer id);

    List<ScheduleExceptionResponse> findBySchedule(Integer scheduleId);

    List<ScheduleExceptionResponse> findBetween(LocalDate from, LocalDate to);

    boolean exists(Integer scheduleId, LocalDate date);
}
