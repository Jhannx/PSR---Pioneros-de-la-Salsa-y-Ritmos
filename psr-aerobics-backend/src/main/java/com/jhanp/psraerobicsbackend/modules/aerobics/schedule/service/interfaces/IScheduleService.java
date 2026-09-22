package com.jhanp.psraerobicsbackend.modules.aerobics.schedule.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.request.ScheduleRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.schedule.dto.response.ScheduleResponse;

import java.util.List;

public interface IScheduleService {

    ScheduleResponse create(ScheduleRequest request);

    ScheduleResponse update(Integer id, ScheduleRequest request);

    ScheduleResponse findById(Integer id);

    List<ScheduleResponse> findAll();

    List<ScheduleResponse> findAllActive();

    /**
     * Día ISO: 1 = lunes … 7 = domingo.
     */
    List<ScheduleResponse> findByDayOfWeek(Integer dayOfWeek);

    /**
     * Un horario nunca se borra: hay sesiones ligadas. Se desactiva.
     */
    ScheduleResponse setActive(Integer id, boolean active);

    /**
     * Fuerza el barrido de creación anticipada de sesiones sobre todos los horarios activos.
     * <p>
     * Es idempotente: lo que ya existe se salta, así que llamarlo de más no cuesta nada.
     *
     * @return cuántas sesiones se crearon
     */
    int generateSessions();
}
