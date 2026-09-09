package com.jhanp.psraerobicsbackend.modules.aerobics.history.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.dto.response.ChangeHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.enums.AuditableTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Consulta de auditoría. No expone escritura: el historial lo alimentan los triggers y los
 * procedimientos, nunca la capa de servicio.
 * <p>
 * Todo sale de vista_historial_cambios, que además resuelve el nombre de quien hizo el cambio. Leer
 * la tabla cruda obligaría al frontend a cruzar contra usuario solo para mostrarlo.
 */
public interface IChangeHistoryService {

    /**
     * Cambios de un registro puntual. Paginado como los otros dos filtros: los tres salen por el
     * mismo endpoint, y mezclar List con Page ahí obligaría al frontend a leer dos formas distintas
     * de la misma respuesta.
     */
    Page<ChangeHistoryViewResponse> findByRecord(AuditableTable affectedTable, Integer recordId, Pageable pageable);

    Page<ChangeHistoryViewResponse> findByUser(Integer userId, Pageable pageable);

    Page<ChangeHistoryViewResponse> findBetween(OffsetDateTime from, OffsetDateTime to, Pageable pageable);
}
