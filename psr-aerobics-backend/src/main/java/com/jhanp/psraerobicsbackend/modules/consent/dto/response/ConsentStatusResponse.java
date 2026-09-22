package com.jhanp.psraerobicsbackend.modules.consent.dto.response;

import java.util.List;

/**
 * Salida de GET /api/personas/{id}/consentimientos: el estado vigente de los cuatro, más el
 * historial completo.
 * <p>
 * Los cuatro booleanos son la lectura rápida —lo que el frontend necesita para pintar casillas— y el
 * historial es la prueba. Van juntos a propósito: separarlos en dos endpoints obligaría a pedir los
 * dos siempre, porque quien pregunta por el estado casi siempre quiere saber también desde cuándo.
 */
public record ConsentStatusResponse(
        Integer personId,

        /** La versión vigente de la política, no la que aceptó la persona. Cada fila lleva la suya. */
        String policyVersion,

        boolean dataProcessing,
        boolean voluntaryParticipation,
        boolean healthData,
        boolean thirdPartyData,

        /**
         * true cuando la persona no tiene <b>ninguna</b> fila de autorización general — ni otorgada ni
         * revocada.
         * <p>
         * Es el caso de todo el mundo que ya estaba registrado antes de que esta tabla existiera, y
         * hay que distinguirlo de "la revocó": no es lo mismo alguien que dijo que no que alguien a
         * quien nunca se le preguntó. A los primeros hay que respetarles la decisión; a los segundos
         * hay que preguntarles.
         * <p>
         * No se rellenaron esas filas en la migración y no deben rellenarse nunca: inventar un
         * consentimiento que nadie dio es peor que no tenerlo, porque convierte el registro entero en
         * algo que no se puede defender.
         */
        boolean registrationConsentMissing,

        /** Del más reciente al más antiguo, con revocaciones incluidas. */
        List<ConsentResponse> history
) {
}
