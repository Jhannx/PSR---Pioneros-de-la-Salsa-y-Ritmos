package com.jhanp.psraerobicsbackend.modules.aerobics.session.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.request.SessionRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionAttendeeResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionResponse;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.dto.response.SessionSummaryViewResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.session.service.interfaces.ISessionService;
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
import com.jhanp.psraerobicsbackend.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Gestión y consulta de sesiones de aeróbicos.
 */
@RestController
@RequestMapping("/api/sesiones")
// Ver sesiones y resúmenes: todo el personal. Abrirlas y cerrarlas también — ver CLASS_OPS.
@PreAuthorize(Authorities.STAFF)
@RequiredArgsConstructor
@Validated
public class SessionController {

    private final ISessionService sessionService;

    /**
     * Creación manual de una sesión por el administrador.
     */
    @PostMapping
    @PreAuthorize(Authorities.CLASS_OPS)
    public ResponseEntity<SessionResponse> createManual(@RequestBody @Valid SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createManual(request));
    }

    /**
     * Cancelación de una sesión. Requiere el usuario que ejecuta la acción para auditoría.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Authorities.CLASS_OPS)
    public ResponseEntity<SessionResponse> cancel(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id,
            Authentication authentication) {
        Integer userId = AuthenticatedUser.requireId(authentication);
        return ResponseEntity.ok(sessionService.cancel(id, userId));
    }

    /**
     * Obtener una sesión por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> findById(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id,
            Authentication authentication) {

        return ResponseEntity.ok(withPermissions(sessionService.findById(id),
                isDesk(authentication), AuthenticatedUser.requireId(authentication)));
    }

    /**
     * Sesiones de una fecha específica.
     */
    @GetMapping("/fecha/{date}")
    public ResponseEntity<List<SessionResponse>> findByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sessionService.findByDate(date));
    }

    /**
     * Listado paginado de sesiones entre fechas, con filtro de estado.
     * <p>
     * {@code ?estado=activas|canceladas|todas}. Por defecto <b>activas</b>: la pantalla de Sesiones
     * muestra la semana en curso, y una clase cancelada no es algo que se quiera ver mezclado con las
     * que se van a dar. El filtro es del servidor y no del cliente porque filtrar despues de paginar
     * devuelve paginas incompletas.
     */
    @GetMapping
    public ResponseEntity<Page<SessionResponse>> findBetween(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false, defaultValue = "activas") String estado,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        return ResponseEntity.ok(withPermissions(
                sessionService.findBetween(desde, hasta, cancelledFilter(estado), pageable),
                authentication));
    }

    /**
     * Las sesiones del profesor que pregunta. Mismo rango y mismo filtro de estado que el listado
     * general.
     * <p>
     * El profesor sale del token y no de la ruta: si viniera en la URL, cualquier profesor podria ver
     * la agenda de otro cambiando el numero.
     */
    @GetMapping("/mias")
    @PreAuthorize(Authorities.CLASS_OPS)
    public ResponseEntity<Page<SessionResponse>> findMine(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        Integer teacherUserId = AuthenticatedUser.requireId(authentication);

        return ResponseEntity.ok(withPermissions(
                sessionService.findByTeacher(teacherUserId, desde, hasta, pageable), authentication));
    }

    /**
     * Quien asistio a una sesion, con nombres.
     * <p>
     * {@code /resumen} solo da totales, y el profesor necesita saber quien esta en su clase — es la
     * razon de que se le asigne una. Abierto a todo el personal: recepcion tambien lo consulta.
     */
    @GetMapping("/{id}/asistentes")
    public ResponseEntity<List<SessionAttendeeResponse>> getAttendees(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id) {

        return ResponseEntity.ok(sessionService.getAttendees(id));
    }

    /**
     * Rellena canClose y canCancel segun quien pregunta.
     * <p>
     * No puede hacerlo el mapper — no conoce al usuario— ni el service sin arrastrar el contexto de
     * seguridad hasta ahi. El controller es el sitio donde las dos cosas estan a la vista.
     * <p>
     * Las reglas replican exactamente lo que hacen los endpoints, que es el punto: un boton que se
     * pinta y luego responde 403 es peor que no pintarlo.
     * <ul>
     *   <li><b>canClose</b>: mostrador siempre, profesor solo en las suyas. Y nunca en una cancelada
     *       ni en una que ya termino, porque ahi cerrar es un no-op.</li>
     *   <li><b>canCancel</b>: cualquiera que opere la clase, mientras no este ya cancelada.</li>
     * </ul>
     */
    private Page<SessionResponse> withPermissions(Page<SessionResponse> page, Authentication authentication) {
        boolean isDesk = isDesk(authentication);
        Integer userId = AuthenticatedUser.requireId(authentication);

        return page.map(session -> withPermissions(session, isDesk, userId));
    }

    private SessionResponse withPermissions(SessionResponse session, boolean isDesk, Integer userId) {
        boolean cancelled = Boolean.TRUE.equals(session.cancelled());
        boolean ended = "ENDED".equals(session.status());
        boolean mine = userId.equals(session.teacherUserId());

        return new SessionResponse(
                session.id(), session.scheduleId(), session.date(), session.startTime(),
                session.endTime(), session.teacherUserId(), session.cancelled(), session.status(),
                session.teacherName(), session.totalCheckins(), session.totalAttended(),
                !cancelled && !ended && (isDesk || mine),
                !cancelled);
    }

    private boolean isDesk(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "RECEPTIONIST".equals(authority));
    }

    /**
     * activas -> las no canceladas; canceladas -> solo esas; todas -> sin filtro.
     * <p>
     * Un valor desconocido se rechaza en vez de caer en un defecto silencioso: si alguien escribe
     * {@code ?estado=activa}, es mejor que lo sepa a que reciba el listado completo creyendo que
     * filtro.
     */
    private Boolean cancelledFilter(String estado) {
        return switch (estado.toLowerCase()) {
            case "activas" -> Boolean.FALSE;
            case "canceladas" -> Boolean.TRUE;
            case "todas" -> null;
            default -> throw new InvalidDataException(
                    "El estado debe ser 'activas', 'canceladas' o 'todas'; llegó: " + estado);
        };
    }

    /**
     * Cierre manual de una sesion: fija su hora de fin en ahora.
     * <p>
     * <b>Un PROFESOR solo puede cerrar las suyas</b>, y por eso el permiso no se puede resolver con una
     * anotacion sola: hay que saber de quien es la sesion. ADMIN y RECEPCION cierran cualquiera.
     * <p>
     * Si la sesion ya habia terminado, no la toca — ver {@code SessionService.close}.
     */
    @PutMapping("/{id}/cerrar")
    @PreAuthorize(Authorities.CLASS_OPS)
    public ResponseEntity<SessionResponse> close(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id,
            Authentication authentication) {

        boolean isDesk = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "RECEPTIONIST".equals(authority));

        if (!isDesk && !sessionService.isTeacherOf(id, AuthenticatedUser.requireId(authentication))) {
            throw new AccessDeniedException("Solo puedes finalizar las sesiones que tienes asignadas");
        }

        return ResponseEntity.ok(sessionService.close(id));
    }

    /**
     * Asignación o cambio de profesor de la sesión.
     */
    @PutMapping("/{id}/profesor/{teacherUserId}")
    @PreAuthorize(Authorities.CLASS_OPS)
    public ResponseEntity<SessionResponse> assignTeacher(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id,
            @PathVariable @Positive(message = "El ID de profesor debe ser positivo") Integer teacherUserId) {
        return ResponseEntity.ok(sessionService.assignTeacher(id, teacherUserId));
    }

    /**
     * Resumen de una sesión (totales de asistencia).
     */
    @GetMapping("/{id}/resumen")
    public ResponseEntity<SessionSummaryViewResponse> getSummary(
            @PathVariable @Positive(message = "El ID de sesión debe ser positivo") Integer id) {
        return sessionService.getSummary(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Resúmenes de sesiones por fecha.
     */
    @GetMapping("/resumenes/fecha/{date}")
    public ResponseEntity<List<SessionSummaryViewResponse>> getSummariesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(sessionService.getSummariesByDate(date));
    }

    /**
     * Resúmenes paginados entre fechas.
     */
    @GetMapping("/resumenes")
    public ResponseEntity<Page<SessionSummaryViewResponse>> getSummariesBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sessionService.getSummariesBetween(desde, hasta, pageable));
    }

}
