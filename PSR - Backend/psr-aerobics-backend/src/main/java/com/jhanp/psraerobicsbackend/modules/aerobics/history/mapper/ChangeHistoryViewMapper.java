package com.jhanp.psraerobicsbackend.modules.aerobics.history.mapper;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.dto.response.ChangeHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.view.ChangeHistoryView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Pasa vista_historial_cambios a su DTO. No hay mapper desde la tabla historial_cambio: la vista es
 * la única lectura que se expone, porque además resuelve el nombre de quien hizo el cambio.
 * <p>
 * previousValue (columna valor_anterior) sale como oldValue, que es como lo lee el frontend.
 */
@Mapper(componentModel = "spring")
public interface ChangeHistoryViewMapper {

    @Mapping(source = "previousValue", target = "oldValue")
    ChangeHistoryViewResponse toResponse(ChangeHistoryView view);
}
