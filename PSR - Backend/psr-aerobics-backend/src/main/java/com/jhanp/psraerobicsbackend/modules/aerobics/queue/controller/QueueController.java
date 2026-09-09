package com.jhanp.psraerobicsbackend.modules.aerobics.queue.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.PrepaidQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.TodayQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.dto.response.WaitingQueueViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.queue.service.interfaces.IQueueService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cola del día. Solo lectura: las filas nacen de sp_registrar_llegada, nunca de un POST.
 * <p>
 * Las rutas de sala de espera están separadas de la de administración a propósito: la de TV no debe
 * poder devolver saldo ni monto aunque alguien se equivoque aquí.
 */
@RestController
@RequestMapping("/api/cola")
@RequiredArgsConstructor
@Validated
public class QueueController {

    private final IQueueService queueService;

    /**
     * Panel de administración: incluye montos, saldo y nota.
     * <p>
     * sesionId es un filtro opcional sobre la misma lista — misma forma de respuesta, menos filas —
     * así que va como query param y no como ruta aparte. La vista ya está acotada al día de hoy.
     */
    @GetMapping("/admin")
    public ResponseEntity<List<TodayQueueViewResponse>> getTodayQueue(
            @RequestParam(name = "sesionId", required = false)
            @Positive(message = "El ID de sesión debe ser positivo") Integer sessionId) {

        if (sessionId == null) {
            return ResponseEntity.ok(queueService.getTodayQueue());
        }
        return ResponseEntity.ok(queueService.getTodayQueueBySession(sessionId));
    }

    /**
     * Pendientes de atender en una sesión.
     * <p>
     * Endpoint propio y no un ?pendientes=true sobre /admin: ese booleano solo tiene sentido
     * acompañado de sesionId, y esa precondición habría que validarla a mano en el controller.
     * Aquí sesionId es obligatorio y no hay combinación inválida posible.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<TodayQueueViewResponse>> getPendingBySession(
            @RequestParam("sesionId") @Positive(message = "El ID de sesión debe ser positivo") Integer sessionId) {

        return ResponseEntity.ok(queueService.getPendingBySession(sessionId));
    }

    /**
     * Pantalla de la sala de espera. Sin datos de dinero (RNF-03).
     */
    @GetMapping("/espera")
    public ResponseEntity<List<WaitingQueueViewResponse>> getWaitingQueue() {
        return ResponseEntity.ok(queueService.getWaitingQueue());
    }

    /**
     * Pantalla de la sala de espera, ya pagados. Cada fila desaparece al vencer
     * minutos_visibilidad_pagos, y eso lo resuelve la vista, no este endpoint.
     */
    @GetMapping("/prepagada")
    public ResponseEntity<List<PrepaidQueueViewResponse>> getPrepaidQueue() {
        return ResponseEntity.ok(queueService.getPrepaidQueue());
    }
}
