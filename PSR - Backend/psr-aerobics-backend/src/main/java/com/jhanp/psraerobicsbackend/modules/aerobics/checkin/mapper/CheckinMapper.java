package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.entity.Checkin;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.view.TodayQueueView;
import com.jhanp.psraerobicsbackend.shared.mapper.PersonNames;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida. No hay toEntity: la fila de llegada la crea sp_registrar_llegada, que además resuelve
 * la sesión del momento. Construirla en Java saltaría esa resolución.
 */
@Mapper(componentModel = "spring", imports = PersonNames.class)
public interface CheckinMapper {

    /**
     * Lectura suelta de la tabla llegada. No incluye posición: la calcula vista_fila_hoy por sesión,
     * no la fila en sí.
     */
    @Mapping(source = "id", target = "checkinId")
    @Mapping(source = "person.id", target = "personId")
    @Mapping(source = "session.id", target = "sessionId")
    @Mapping(target = "fullName", expression = "java(PersonNames.fullName(checkin.getPerson()))")
    @Mapping(target = "queuePosition", ignore = true)
    CheckinResponse toResponse(Checkin checkin);

    /**
     * Respuesta de los endpoints de check-in (patrón H): la vista resuelve de una sola lectura el
     * nombre, la sesión y la posición que el procedimiento no devuelve.
     */
    @Mapping(source = "position", target = "queuePosition")
    @Mapping(target = "fullName", expression = "java(fullName(row))")
    CheckinResponse toResponse(TodayQueueView row);

    default String fullName(TodayQueueView row) {
        if (row == null) {
            return null;
        }
        String first = row.getFirstName() != null ? row.getFirstName() : "";
        String last = row.getLastName() != null ? row.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}
