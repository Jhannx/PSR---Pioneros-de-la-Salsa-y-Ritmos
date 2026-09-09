package com.jhanp.psraerobicsbackend.modules.aerobics.history.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.history.dto.response.ChangeHistoryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.enums.AuditableTable;
import com.jhanp.psraerobicsbackend.modules.aerobics.history.service.interfaces.IChangeHistoryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Auditoría. Solo lectura: el historial lo alimentan los triggers y los procedimientos, nunca un
 * endpoint.
 */
@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
@Validated
public class ChangeHistoryController {

    private final IChangeHistoryService changeHistoryService;

    /**
     * Historial de cambios de un registro puntual en una tabla auditable.
     */
    @GetMapping("/historial-cambios/registro/{tabla}/{registroId}")
    public ResponseEntity<Page<ChangeHistoryViewResponse>> findChangesByRecord(
            @PathVariable("tabla") AuditableTable affectedTable,
            @PathVariable("registroId") @Positive(message = "El ID de registro debe ser positivo") Integer recordId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(changeHistoryService.findByRecord(affectedTable, recordId, pageable));
    }

    /**
     * Historial de cambios realizados por un usuario específico.
     */
    @GetMapping("/historial-cambios/usuario/{usuarioId}")
    public ResponseEntity<Page<ChangeHistoryViewResponse>> findChangesByUser(
            @PathVariable("usuarioId") @Positive(message = "El ID de usuario debe ser positivo") Integer userId,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(changeHistoryService.findByUser(userId, pageable));
    }

    /**
     * Historial de cambios dentro de un rango de fechas y horas.
     */
    @GetMapping("/historial-cambios")
    public ResponseEntity<Page<ChangeHistoryViewResponse>> findChangesBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(changeHistoryService.findBetween(desde, hasta, pageable));
    }
}
