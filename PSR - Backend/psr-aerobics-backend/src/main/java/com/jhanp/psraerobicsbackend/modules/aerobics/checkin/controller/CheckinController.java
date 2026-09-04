package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinByDocumentRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.interfaces.ICheckinService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestión de llegadas (check-in) a las sesiones de aeróbicos.
 */
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@Validated
public class CheckinController {

    private final ICheckinService checkinService;

    /**
     * Registro vía QR. El método se fija como QR.
     */
    @PostMapping("/qr")
    public ResponseEntity<CheckinResponse> registerQr(@RequestBody @Valid CheckinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkinService.register(request, CheckinMethod.QR));
    }

    /**
     * Registro manual por el administrador. El método se fija como MANUAL.
     */
    @PostMapping("/manual")
    public ResponseEntity<CheckinResponse> registerManual(@RequestBody @Valid CheckinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkinService.register(request, CheckinMethod.MANUAL));
    }

    /**
     * Registro en kiosko por número de documento. El método se fija como DOCUMENTO.
     */
    @PostMapping("/documento")
    public ResponseEntity<CheckinResponse> registerByDocument(@RequestBody @Valid CheckinByDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkinService.registerByDocument(request));
    }

    /**
     * Obtener una llegada por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CheckinResponse> findById(
            @PathVariable @Positive(message = "El ID de llegada debe ser positivo") Integer id) {
        return ResponseEntity.ok(checkinService.findById(id));
    }

    /**
     * Listado de llegadas por sesión.
     */
    @GetMapping("/sesion/{sessionId}")
    public ResponseEntity<List<CheckinResponse>> findBySession(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer sessionId) {
        return ResponseEntity.ok(checkinService.findBySession(sessionId));
    }

    /**
     * Listado de llegadas por fecha.
     */
    @GetMapping("/fecha/{date}")
    public ResponseEntity<List<CheckinResponse>> findByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(checkinService.findByDate(date));
    }

    /**
     * Historial de llegadas de una persona.
     */
    @GetMapping("/persona/{personId}")
    public ResponseEntity<Page<CheckinResponse>> findByPerson(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer personId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(checkinService.findByPerson(personId, pageable));
    }
}
