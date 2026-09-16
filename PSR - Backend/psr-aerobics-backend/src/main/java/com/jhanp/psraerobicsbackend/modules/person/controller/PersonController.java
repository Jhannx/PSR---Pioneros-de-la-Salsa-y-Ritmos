package com.jhanp.psraerobicsbackend.modules.person.controller;

import com.jhanp.psraerobicsbackend.auth.dto.request.EmailChangeConfirmRequest;
import com.jhanp.psraerobicsbackend.auth.dto.request.EmailChangeRequest;
import com.jhanp.psraerobicsbackend.auth.service.IEmailChangeService;
import com.jhanp.psraerobicsbackend.auth.service.RegisterService;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.HealthObservationsRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonUpdateRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonProfileViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.TerminalPersonLookupResponse;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IPersonService;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import com.jhanp.psraerobicsbackend.shared.response.MessageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import com.jhanp.psraerobicsbackend.security.Authorities;
import com.jhanp.psraerobicsbackend.shared.validation.SearchTerms;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.jhanp.psraerobicsbackend.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alta, consulta y baja de personas. Toda persona registrada nace con cuenta de acceso, así que no
 * hay un endpoint aparte para crearla.
 */
@RestController
@RequestMapping("/api/personas")
// Consultar personas lo necesita todo el personal; darlas de alta y de baja es del mostrador. El
// autoregistro es la excepción y va marcado a mano, porque quien lo usa no tiene cuenta todavía.
@PreAuthorize(Authorities.STAFF)
@RequiredArgsConstructor
@Validated
public class PersonController {

    private final IPersonService personService;

    /**
     * El alta completa —persona, cuenta y correo de bienvenida— la coordina RegisterService, no
     * PersonService: es el único punto donde esas tres cosas ocurren en la misma transacción.
     */
    private final RegisterService registerService;

    /**
     * La cuenta de acceso de la persona. Aquí solo para el reinicio de contraseña del mostrador: el
     * resto de operaciones sobre cuentas vive en /api/usuarios.
     */
    private final IUserService userService;

    /** El cambio de correo: emite el token y lo canjea. */
    private final IEmailChangeService emailChangeService;

    /**
     * Listado paginado. Con ?buscar= filtra por nombre, apellidos o número de documento, en un único
     * campo de texto: coincidencia parcial, sin distinguir mayúsculas ni tildes.
     * <p>
     * El filtro se aplica en el servidor, nunca en el cliente. Filtrar en el front solo alcanzaría a
     * la página ya cargada y daría resultados incompletos con la paginación puesta.
     */
    @GetMapping
    public ResponseEntity<Page<PersonResponse>> findAll(
            @RequestParam(required = false) String buscar,
            @PageableDefault(size = 20) Pageable pageable) {

        String term = SearchTerms.normalize(buscar);

        Page<PersonResponse> personas = (term == null)
                ? personService.findAll(pageable)
                : personService.search(term, pageable);

        return ResponseEntity.ok(personas);
    }

    @GetMapping("/buscar")
    @PreAuthorize(Authorities.PERSON_LOOKUP)
    public ResponseEntity<TerminalPersonLookupResponse> findByDocument(
            @RequestParam("documento") String documentNumber) {

        PersonResponse person = personService.findByDocumentNumber(documentNumber);
        return ResponseEntity.ok(new TerminalPersonLookupResponse(
                person.id(), person.documentNumber(), person.firstName(), person.lastName(), person.active()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_SELF)
    public ResponseEntity<PersonResponse> findById(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(personService.findById(id));
    }

    /**
     * Registro hecho por un administrador.
     * <p>
     * La fuente de registro la fija la ruta y nunca el body: si viniera del cliente, cualquiera
     * podría marcar un registro manual como autoregistro.
     */
    @PostMapping
    @PreAuthorize(Authorities.DESK)
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerService.registerByAdmin(request));
    }

    /**
     * Registro que hace la propia persona desde el kiosko. Es público: quien lo usa todavía no tiene
     * cuenta.
     * <p>
     * El permitAll explícito es obligatorio: la clase entera lleva @PreAuthorize(STAFF), y sin esta
     * anotación la ruta respondería 403 aunque SecurityConfig la deje pasar — el filtro y la anotación
     * son dos puertas distintas, y hay que abrir las dos.
     */
    @PostMapping("/autoregistro")
    @PreAuthorize("permitAll()")
    public ResponseEntity<PersonResponse> selfRegister(@Valid @RequestBody PersonRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerService.registerSelf(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Authorities.DESK)
    public ResponseEntity<PersonResponse> update(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @Valid @RequestBody PersonUpdateRequest request) {

        return ResponseEntity.ok(personService.update(id, request));
    }

    /**
     * Desactiva a la persona. El trigger de sincronización desactiva también su cuenta.
     */
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize(Authorities.DESK)
    public ResponseEntity<Void> deactivate(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        personService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva a la persona. Queda auditado, por eso hace falta saber quién lo hizo.
     * <p>
     * El usuario que reactiva sale del contexto de autenticación (JWT).
     */
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(Authorities.DESK)
    public ResponseEntity<PersonResponse> reactivate(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            Authentication authentication) {

        Integer userId = AuthenticatedUser.requireId(authentication);
        return ResponseEntity.ok(personService.reactivate(id, userId));
    }

    /**
     * Eliminación definitiva e irreversible: borra persona y cuenta, y reasigna su huella
     * (llegadas, movimientos, asistencias) al registro sentinel.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            Authentication authentication) {

        Integer userId = AuthenticatedUser.requireId(authentication);
        personService.deletePermanently(id, userId);

        return ResponseEntity.noContent().build();
    }


    /**
     * Perfil consolidado: datos, contacto de emergencia, saldo, días de crédito y resumen de
     * asistencias en una sola consulta.
     */
    @GetMapping("/{id}/perfil")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_SELF)
    public ResponseEntity<PersonProfileViewResponse> getProfile(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(personService.getProfile(id));
    }

    /**
     * Saldo a favor. Negativo significa que la persona debe.
     */
    @GetMapping("/{id}/saldo")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_SELF)
    public ResponseEntity<PersonBalanceViewResponse> getBalance(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(personService.getBalance(id));
    }

    /**
     * Alergias, condiciones y demás notas de salud.
     * <p>
     * Endpoint propio y no parte del PUT de persona por permisos: aquel es de mostrador y reescribe
     * documento, nombre y teléfono, así que abrirlo al propio miembro para que editara una nota de
     * salud le dejaría también cambiarse el documento. Este solo toca esa columna, y por eso se puede
     * abrir al dueño.
     * <p>
     * Antes vivía en el contacto de emergencia, cuando había uno solo por persona; con hasta cinco
     * contactos no tendría dueño claro.
     */
    @PutMapping("/{id}/observaciones-salud")
    @PreAuthorize(Authorities.DESK + Authorities.OR_SELF)
    public ResponseEntity<PersonResponse> updateHealthObservations(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @Valid @RequestBody HealthObservationsRequest request) {

        return ResponseEntity.ok(personService.updateHealthObservations(id, request.healthObservations()));
    }

    /**
     * Devuelve la contraseña de la persona a su número de documento.
     * <p>
     * Solo mostrador (ADMIN/RECEPCIÓN), y sin token de por medio: quien lo ejecuta tiene delante a la
     * persona y su documento físico, exactamente igual que cuando la registró. Es la única salida
     * para quien se dio de alta sin correo —el caso normal de la Terminal PSR—, que nunca podrá usar
     * el flujo de "olvidé mi contraseña".
     */
    @PatchMapping("/{id}/reiniciar-password")
    @PreAuthorize(Authorities.DESK)
    public ResponseEntity<MessageResponse> resetPassword(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        userService.resetPasswordToDocument(id);

        // El documento no viaja en la respuesta: quien llama ya lo tiene en pantalla, y así no queda
        // una contraseña en claro en el historial del navegador ni en los logs de red.
        return ResponseEntity.ok(new MessageResponse(
                "La contraseña volvió a ser el número de documento de la persona."));
    }

    /**
     * Pide cambiar el correo de la cuenta. No lo cambia: manda un enlace de confirmación al correo
     * NUEVO, y hasta que no se canjee sigue mandando el de siempre.
     * <p>
     * La propia persona o el mostrador en su nombre. Un PUT directo sobre el correo sería una vía de
     * secuestro de cuentas —cambiarlo al propio y recuperar la contraseña desde ahí—, y por eso este
     * endpoint no escribe nada en usuario.
     */
    @PostMapping("/{id}/solicitar-cambio-correo")
    @PreAuthorize(Authorities.DESK + Authorities.OR_SELF)
    public ResponseEntity<MessageResponse> requestEmailChange(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @Valid @RequestBody EmailChangeRequest request) {

        return ResponseEntity.ok(emailChangeService.request(id, request));
    }

    /**
     * Canjea el enlace y aplica el correo nuevo.
     * <p>
     * permitAll explícito, como el autoregistro: quien abre el enlace desde su buzón puede no tener
     * sesión abierta, y sin esto la clase entera exigiría STAFF. La autorización la da el token, que
     * es de un solo uso y caduca. También está en la lista de rutas públicas de SecurityConfig: sin
     * las dos cosas, la cadena de filtros lo rechaza antes de llegar hasta aquí.
     */
    @PostMapping("/confirmar-cambio-correo")
    @PreAuthorize("permitAll()")
    public ResponseEntity<MessageResponse> confirmEmailChange(
            @Valid @RequestBody EmailChangeConfirmRequest request) {

        return ResponseEntity.ok(emailChangeService.confirm(request));
    }
}
