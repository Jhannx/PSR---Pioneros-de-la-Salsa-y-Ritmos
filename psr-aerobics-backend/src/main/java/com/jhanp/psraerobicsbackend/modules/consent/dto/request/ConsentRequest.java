package com.jhanp.psraerobicsbackend.modules.consent.dto.request;

import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada de POST /api/personas/{id}/consentimientos.
 * <p>
 * <b>Este endpoint no es por donde pasa el flujo normal.</b> Los consentimientos del día a día se
 * graban dentro de la operación que los motiva —el alta, guardar una observación de salud, añadir un
 * contacto— y en la misma transacción, que es lo único que garantiza que no quede una cosa sin la
 * otra. Este endpoint existe para los dos casos que se salen de ahí:
 * <ul>
 *   <li>Las personas registradas antes de que esto existiera, a las que hay que preguntarles.</li>
 *   <li>Revocar después, que es un derecho del artículo 8 y no tiene operación propia que lo
 *       acompañe.</li>
 * </ul>
 * <p>
 * No hay campo de versión: la pone el servidor con la vigente. Si viniera del cliente, cualquiera
 * podría dejar constancia de haber aceptado un texto que nunca se le mostró.
 */
public record ConsentRequest(
        @NotNull(message = "El tipo de consentimiento es obligatorio")
        ConsentType type,

        @NotNull(message = "Hay que indicar si se otorga o se revoca")
        Boolean granted,

        /**
         * En el JSON se escribe {@code "DIGITAL"} o {@code "PHYSICAL"} — el nombre de la constante, no
         * el valor que se guarda en la base ("FISICO"), que traduce el converter.
         * <p>
         * Es declarativo: el servidor no puede comprobar que exista un papel firmado, igual que no
         * puede comprobar que la persona leyó lo que marcó. Lo que sí queda registrado, y es lo que
         * importa, es quién lo afirmó — va en registrado_por.
         */
        @NotNull(message = "El medio del consentimiento es obligatorio")
        ConsentMedium medium
) {
}
