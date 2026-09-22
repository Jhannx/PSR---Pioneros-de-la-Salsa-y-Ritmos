package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinByDocumentRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.OwnCheckinResponse;
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

    /**
     * Variante de mostrador: resuelve la persona por documento pero deja el metodo MANUAL.
     * <p>
     * El "Agregar manual" de recepcion se registraba por el endpoint de la terminal, asi que quedaba
     * como DOCUMENTO y en pantalla salia "Terminal PSR" — cuando lo habia tecleado una persona del
     * mostrador. Ademas MANUAL es el unico metodo que puede entrar fuera de horario.
     */
    CheckinResponse registerManualByDocument(CheckinByDocumentRequest request);

    /**
     * La llegada de hoy de una persona, si la tiene. Vacio si hoy no ha llegado.
     * <p>
     * Lo que permite que la pantalla del miembro sobreviva a un F5: sin esto, la llegada solo existia
     * en la memoria del navegador.
     */
    java.util.Optional<OwnCheckinResponse> findTodayOwn(Integer personId);

    /**
     * Marca la llegada de la propia persona, y es seguro de repetir.
     * <p>
     * Si ya tiene una llegada de hoy en una sesion que no ha terminado, devuelve esa misma en vez de
     * crear otra; si la sesion de su llegada anterior ya acabo, si registra una nueva — es el caso de
     * quien viene a dos clases el mismo dia.
     *
     * @return la llegada y si ya existia, para que el controller responda 200 en vez de 201
     */
    OwnCheckinResult registerOwn(Integer personId);

    /** Lo que devuelve {@link #registerOwn}: la llegada, y si ya estaba registrada. */
    record OwnCheckinResult(OwnCheckinResponse checkin, boolean alreadyExisted) {
    }

    CheckinResponse findById(Integer id);

    List<CheckinResponse> findBySession(Integer sessionId);

    List<CheckinResponse> findByDate(LocalDate date);

    Page<CheckinResponse> findByPerson(Integer personId, Pageable pageable);

    boolean hasCheckedIn(Integer personId, Integer sessionId);
}
