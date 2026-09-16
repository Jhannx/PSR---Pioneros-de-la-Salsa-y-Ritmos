package com.jhanp.psraerobicsbackend.modules.aerobics.payment.controller;

import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.request.PersonRefundRequest;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.dto.response.PersonRefundResponse;
import com.jhanp.psraerobicsbackend.modules.aerobics.payment.service.interfaces.IAccountMovementService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reembolso a nivel de persona.
 * <p>
 * Controller aparte y no un método más en {@link AccountMovementController} porque la ruta cuelga de
 * /api/personas y el @RequestMapping de aquel es /api/movimientos. La lógica sigue viviendo en el
 * módulo de pagos, que es donde corresponde: lo único que cambia es por qué puerta se entra.
 * <p>
 * Misma autorización que el resto de movimientos: el mostrador. Devolver dinero no es del profesor.
 */
@RestController
@RequestMapping("/api/personas")
@PreAuthorize(Authorities.DESK)
@RequiredArgsConstructor
@Validated
public class PersonRefundController {

    private final IAccountMovementService accountMovementService;

    /**
     * Devuelve un monto a la persona repartiéndolo entre sus abonos con saldo, del más antiguo al más
     * reciente, en una sola transacción.
     * <p>
     * Responde 400 si el monto supera lo que queda por devolver, con el disponible en el mensaje. El
     * usuario que lo registra sale del JWT, no del cuerpo.
     */
    @PostMapping("/{personId}/reembolso")
    public ResponseEntity<PersonRefundResponse> refund(
            @PathVariable @Positive(message = "El ID de persona debe ser positivo") Integer personId,
            @RequestBody @Valid PersonRefundRequest request,
            Authentication authentication) {

        Integer userId = AuthenticatedUser.requireId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountMovementService.refundByPerson(personId, request, userId));
    }
}
