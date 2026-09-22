package com.jhanp.psraerobicsbackend.modules.consent.controller;

import com.jhanp.psraerobicsbackend.modules.consent.dto.request.ConsentRequest;
import com.jhanp.psraerobicsbackend.modules.consent.dto.response.ConsentResponse;
import com.jhanp.psraerobicsbackend.modules.consent.dto.response.ConsentStatusResponse;
import com.jhanp.psraerobicsbackend.modules.consent.service.interfaces.IConsentService;
import com.jhanp.psraerobicsbackend.security.AuthenticatedUser;
import com.jhanp.psraerobicsbackend.security.Authorities;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta y registro manual de consentimientos.
 * <p>
 * <b>Estos dos endpoints no son la vía normal.</b> El consentimiento del día a día viaja dentro de la
 * operación que lo motiva — el alta lo graba en {@code RegisterService}, la observación de salud en
 * {@code PersonService}, el contacto de emergencia en {@code EmergencyContactService} — y siempre en
 * la misma transacción. Un endpoint aparte al que el frontend llamara "justo antes o justo después"
 * reintroduciría el agujero que todo esto vino a tapar: dos llamadas separadas se pueden partir por
 * la mitad, y entonces queda una persona sin constancia o una constancia sin persona.
 * <p>
 * Lo que sí necesita puerta propia son los dos casos que no tienen operación asociada: preguntarle a
 * quien ya estaba registrado antes de que esto existiera, y revocar más tarde.
 * <p>
 * Sin {@code @RequestMapping} de clase, igual que {@code EmergencyContactController}: las rutas
 * cuelgan de /api/personas, que ya tiene dueño en {@code PersonController}, y declararlas enteras en
 * cada método evita que dos clases se disputen el mismo prefijo.
 */
@RestController
@PreAuthorize(Authorities.STAFF)
@RequiredArgsConstructor
@Validated
public class ConsentController {

    private final IConsentService consentService;

    /**
     * Estado vigente de los cuatro consentimientos, más el historial completo con sus fechas.
     * <p>
     * Abierto a la propia persona y no solo al personal: el numeral 10 de la política le reconoce el
     * derecho a conocer qué datos suyos hay, y qué autorizó y cuándo es uno de ellos.
     */
    @GetMapping("/api/personas/{id}/consentimientos")
    @PreAuthorize(Authorities.STAFF + Authorities.OR_SELF)
    public ResponseEntity<ConsentStatusResponse> getStatus(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id) {

        return ResponseEntity.ok(consentService.getStatus(id));
    }

    /**
     * Deja constancia de una decisión suelta: la de quien ya estaba registrado, o una revocación.
     * <p>
     * DESK + OR_SELF, no ADMIN: revocar es un derecho del titular (artículo 8 de la Ley 1581) y
     * exigirle que pase por un administrador para ejercerlo sería ponerle una barrera a algo que la
     * ley le garantiza.
     * <p>
     * Siempre 201: nunca actualiza una fila anterior. Incluso revocar y volver a otorgar deja las dos
     * filas, porque las dos ocurrieron.
     */
    @PostMapping("/api/personas/{id}/consentimientos")
    @PreAuthorize(Authorities.DESK + Authorities.OR_SELF)
    public ResponseEntity<ConsentResponse> record(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer id,
            @Valid @RequestBody ConsentRequest request,
            Authentication authentication) {

        // Aquí siempre hay sesión —la ruta exige token—, así que registrado_por nunca queda null por
        // esta vía. El null se reserva para el autoregistro, que es público.
        Integer registeredByUserId = AuthenticatedUser.requireId(authentication);

        ConsentResponse response = consentService.record(
                id, request.type(), request.granted(), request.medium(), registeredByUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
