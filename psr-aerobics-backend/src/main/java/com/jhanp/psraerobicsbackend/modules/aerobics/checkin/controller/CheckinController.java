package com.jhanp.psraerobicsbackend.modules.aerobics.checkin.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinByDocumentRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.request.CheckinRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.CheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.dto.response.OwnCheckinResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.enums.CheckinMethod;
import com.jhanp.psraerobicsbackend.modules.aerobics.checkin.service.interfaces.ICheckinService;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.security.userdetails.model.CustomUserDetails;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import com.jhanp.psraerobicsbackend.security.Authorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestión de llegadas (check-in) a las sesiones de aeróbicos.
 */
@RestController
@RequestMapping("/api/checkin")
// Registrar llegadas es operar la clase: lo hace quien esté en la puerta, sea recepción o el profesor.
@PreAuthorize(Authorities.CLASS_OPS)
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
     * <p>
     * TERMINAL_QUEUE y no el CLASS_OPS de la clase: la Terminal PSR también atiende a quien llega y
     * su documento no aparece (persona sin documento a mano, tecleo fallido), y con el permiso de
     * clase recibía 403.
     */
    @PostMapping("/manual")
    @PreAuthorize(Authorities.TERMINAL_QUEUE)
    public ResponseEntity<CheckinResponse> registerManual(@RequestBody @Valid CheckinRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkinService.register(request, CheckinMethod.MANUAL));
    }

    /**
     * Registro en kiosko por número de documento. El método se fija como DOCUMENTO.
     * <p>
     * Es literalmente la función de la Terminal PSR, así que PSR_TERMINAL entra aquí: quedaba fuera
     * solo porque heredaba el CLASS_OPS de la clase, y con él la terminal no podía registrar ni una
     * llegada.
     */
    @PostMapping("/documento")
    @PreAuthorize(Authorities.TERMINAL_QUEUE)
    public ResponseEntity<CheckinResponse> registerByDocument(@RequestBody @Valid CheckinByDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkinService.registerByDocument(request));
    }

    /**
     * Autoservicio: el miembro marca su propia llegada desde su pantalla de inicio.
     * <p>
     * Sin cuerpo, y es lo que lo hace seguro. La alternativa —abrir /api/cola a MIEMBRO— recibe
     * personId en el body, así que un miembro podía marcarle la llegada a otro con solo cambiar el
     * número. Aquí la persona sale del token y no hay nada que falsificar.
     * <p>
     * El metodo se guarda como PROPIO. Antes se reutilizaba DOCUMENTO para no tocar el enum de
     * Postgres, y eso hacia que el check-in de recepcion mostrara "Terminal PSR" para alguien que
     * habia marcado su llegada desde el telefono.
     * <p>
     * <b>Seguro de repetir.</b> Responde 201 la primera vez y 200 devolviendo la misma llegada si ya
     * la tenia, en vez de crear una segunda. La distincion es util al cliente: 200 significa "ya
     * estabas", que es justo lo que la pantalla necesita saber tras un reintento o un doble toque.
     */
    @PostMapping("/propio")
    @PreAuthorize(Authorities.MEMBER)
    public ResponseEntity<OwnCheckinResponse> registerOwn(@AuthenticationPrincipal CustomUserDetails principal) {
        ICheckinService.OwnCheckinResult result = checkinService.registerOwn(ownPersonId(principal));

        return ResponseEntity
                .status(result.alreadyExisted() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(result.checkin());
    }

    /**
     * La llegada de hoy de quien pregunta, o 204 si hoy no ha llegado.
     * <p>
     * Sin esto, un miembro no tenia forma de saber si ya habia marcado su llegada: la unica lectura de
     * la cola es de personal y a el le responde 403, asi que su pantalla solo lo recordaba en memoria
     * y al recargar volvia a ofrecerle el boton como si no hubiera hecho nada.
     * <p>
     * 204 y no 404: "hoy no has llegado" es una respuesta normal a una pregunta valida, no un error.
     */
    @GetMapping("/propio")
    @PreAuthorize(Authorities.MEMBER)
    public ResponseEntity<OwnCheckinResponse> findTodayOwn(@AuthenticationPrincipal CustomUserDetails principal) {
        return checkinService.findTodayOwn(ownPersonId(principal))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * El "Agregar manual" de recepcion, por numero de documento.
     * <p>
     * Existe porque ese boton llamaba al endpoint de la terminal, asi que la llegada quedaba como
     * DOCUMENTO y en pantalla salia "Terminal PSR" — cuando la habia tecleado una persona del
     * mostrador. Ademas MANUAL es el unico metodo que puede registrar una llegada fuera de horario:
     * quien atiende tiene delante a la persona y ve el caso, cosa que un canal automatico no puede.
     */
    @PostMapping("/manual/documento")
    @PreAuthorize(Authorities.DESK)
    public ResponseEntity<CheckinResponse> registerManualByDocument(
            @RequestBody @Valid CheckinByDocumentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkinService.registerManualByDocument(request));
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
    @PreAuthorize(Authorities.CLASS_OPS + Authorities.OR_SELF_PERSON_ID)
    public ResponseEntity<Page<CheckinResponse>> findByPerson(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer personId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(checkinService.findByPerson(personId, pageable));
    }

    /**
     * La persona detrás de la cuenta autenticada. Puede no haberla —las cuentas técnicas no tienen
     * persona—, y entonces no hay llegada que registrar: 400 con un mensaje claro y no un fallo más
     * abajo con el id en null.
     */
    private Integer ownPersonId(CustomUserDetails principal) {
        Person person = principal != null ? principal.getUser().getPerson() : null;

        if (person == null) {
            throw new InvalidDataException("Tu cuenta no tiene una persona asociada, no puede marcar llegada");
        }
        return person.getId();
    }
}
