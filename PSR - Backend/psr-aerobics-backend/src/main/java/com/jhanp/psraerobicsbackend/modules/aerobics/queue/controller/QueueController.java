package com.jhanp.psraerobicsbackend.modules.aerobics.queue.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.interfaces.ICheckinService;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PendingQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PrepaidQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.TodayQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.WaitingQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces.IQueueService;
import com.jhanp.psraerobicsbackend.security.Authorities;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Cola del día. Las vistas públicas no exponen montos ni saldos. */
@RestController
@RequestMapping("/api/cola")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Validated
public class QueueController {

    private final IQueueService queueService;
    private final ICheckinService checkinService;

    /** Panel administrativo: incluye datos económicos. La terminal no puede acceder. */
    @GetMapping("/admin")
    @PreAuthorize(Authorities.STAFF)
    public ResponseEntity<List<TodayQueueViewResponse>> getTodayQueue(
            @RequestParam(name = "sesionId", required = false)
            @Positive(message = "El ID de sesión debe ser positivo") Integer sessionId) {

        if (sessionId == null) {
            return ResponseEntity.ok(queueService.getTodayQueue());
        }
        return ResponseEntity.ok(queueService.getTodayQueueBySession(sessionId));
    }

    @GetMapping("/pendientes")
    @PreAuthorize(Authorities.TERMINAL_QUEUE)
    public ResponseEntity<List<PendingQueueViewResponse>> getPendingBySession(
            @RequestParam("sesionId") @Positive(message = "El ID de sesión debe ser positivo") Integer sessionId) {
        List<PendingQueueViewResponse> pending = queueService.getPendingBySession(sessionId).stream()
                .map(row -> new PendingQueueViewResponse(row.checkinId(), row.sessionId(), row.personId(),
                        row.firstName(), row.lastName(), row.arrivalTime(), row.checkinMethod(),
                        row.position(), row.attended()))
                .toList();
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/espera")
    @PreAuthorize(Authorities.TERMINAL_QUEUE)
    public ResponseEntity<List<WaitingQueueViewResponse>> getWaitingQueue() {
        return ResponseEntity.ok(queueService.getWaitingQueue());
    }

    @GetMapping("/prepagada")
    @PreAuthorize(Authorities.TERMINAL_QUEUE)
    public ResponseEntity<List<PrepaidQueueViewResponse>> getPrepaidQueue() {
        return ResponseEntity.ok(queueService.getPrepaidQueue());
    }

    /** Registro de llegada de la terminal; el método DOCUMENTO se decide en el servidor. */
    @PostMapping
    @PreAuthorize(Authorities.TERMINAL_QUEUE)
    public ResponseEntity<CheckinResponse> registerTerminalCheckin(
            @Valid @RequestBody CheckinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkinService.register(request, CheckinMethod.DOCUMENT));
    }
}