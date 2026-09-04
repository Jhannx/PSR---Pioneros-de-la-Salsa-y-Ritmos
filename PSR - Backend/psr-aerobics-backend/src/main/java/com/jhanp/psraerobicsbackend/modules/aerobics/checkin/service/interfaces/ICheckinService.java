package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinByDocumentRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ICheckinService {

    /**
     * Registra la llegada vía sp_registrar_llegada, para /api/checkin/qr y /api/checkin/manual.
     * Es idempotente: si la persona ya está en la fila de la sesión vigente, devuelve la existente.
     * <p>
     * checkinMethod va como parámetro y no dentro del request porque lo decide la ruta, no el
     * cliente. La sesión tampoco se recibe: la resuelve fn_obtener_o_crear_sesion_del_momento().
     * <p>
     * El procedimiento solo devuelve el id de la llegada, así que la implementación debe leer
     * vista_fila_hoy en la misma transacción para completar nombre, sesión y posición (patrón H).
     * <p>
     * Puede propagar SQLSTATE 75001 (persona desactivada) y 75002 (no hay clase vigente); son dos
     * mensajes distintos para el usuario final y se traducen en el manejador global.
     */
    CheckinResponse register(CheckinRequest request, CheckinMethod checkinMethod);

    /**
     * Variante de kiosko (/api/checkin/documento): resuelve la persona por número de documento y
     * fija el método DOCUMENTO. 404 si la persona no existe, para que el kiosko ofrezca registrarla.
     */
    CheckinResponse registerByDocument(CheckinByDocumentRequest request);

    CheckinResponse findById(Integer id);

    List<CheckinResponse> findBySession(Integer sessionId);

    List<CheckinResponse> findByDate(LocalDate date);

    Page<CheckinResponse> findByPerson(Integer personId, Pageable pageable);

    boolean hasCheckedIn(Integer personId, Integer sessionId);
}
