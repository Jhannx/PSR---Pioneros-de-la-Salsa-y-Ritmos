package com.jhanp.psraerobicsbackend.modules.consent.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.consent.dto.response.ConsentResponse;
import com.jhanp.psraerobicsbackend.modules.consent.dto.response.ConsentStatusResponse;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;

/**
 * El registro probatorio de autorizaciones. Tres operaciones y ninguna más: dejar constancia,
 * preguntar si la hay, y enseñarla entera.
 * <p>
 * No hay {@code update} ni {@code delete}, y no es un olvido — ver {@code Consent}.
 */
public interface IConsentService {

    /**
     * Deja constancia de una decisión. Siempre inserta: {@code granted = false} es una revocación, no
     * un borrado de lo anterior.
     * <p>
     * Sin transacción propia a propósito (participa en la de quien llama). Es lo que hace que el alta
     * y su consentimiento sean atómicos: si la constancia no se guarda, la persona tampoco queda
     * registrada. Ese es justo el agujero que el frontend describía — marcar la casilla y que el
     * proceso se caiga después sin dejar rastro.
     *
     * @param registeredByUserId null si lo marcó la propia persona sin sesión (autoregistro)
     */
    ConsentResponse record(Integer personId,
                           ConsentType type,
                           boolean granted,
                           ConsentMedium medium,
                           Integer registeredByUserId);

    /**
     * ¿La persona tiene ese consentimiento vigente ahora mismo?
     * <p>
     * false tanto si lo revocó como si nunca se le preguntó. Para las validaciones de los endpoints
     * los dos casos son el mismo —no hay autorización, no se guarda el dato—; distinguirlos solo
     * importa para saber a quién hay que preguntarle, y eso lo dice {@link #getStatus}.
     */
    boolean hasActive(Integer personId, ConsentType type);

    /** Estado vigente de los cuatro tipos más el historial completo. 404 si la persona no existe. */
    ConsentStatusResponse getStatus(Integer personId);
}
