package com.jhanp.psraerobicsbackend.modules.aerobics.history.dto.response;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.enums.AuditableTable;

import java.time.OffsetDateTime;

/**
 * Salida de GET /api/auditoria/historial-cambios, desde vista_historial_cambios.
 * <p>
 * Se lee de la vista y no de historial_cambio porque la vista ya resuelve performedBy — el nombre de
 * quien hizo el cambio. Con la tabla cruda el frontend tendría que cruzar contra usuario solo para
 * mostrarlo. De ahí el nombre: el origen es ChangeHistoryView, no la entidad ChangeHistory.
 * <p>
 * Solo lectura: las filas las escriben los triggers de auditoría y los procedimientos, nunca la capa
 * de servicio. Por eso el módulo no tiene ningún Request.
 */
public record ChangeHistoryViewResponse(
        Integer id,
        AuditableTable affectedTable,
        Integer recordId,
        String field,
        String oldValue,
        String newValue,
        Integer userId,
        String performedBy,
        OffsetDateTime dateTime
) {
}
