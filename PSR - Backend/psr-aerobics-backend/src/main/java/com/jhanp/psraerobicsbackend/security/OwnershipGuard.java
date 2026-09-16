package com.jhanp.psraerobicsbackend.security;

import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.person.repository.EmergencyContactRepository;
import com.jhanp.psraerobicsbackend.security.userdetails.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Responde si quien pide un recurso por persona es esa misma persona.
 * <p>
 * Los roles dicen QUÉ puede hacer alguien, pero no SOBRE QUIÉN. Sin esto,
 * {@code hasAuthority('MEMBER')} dejaría que cualquier miembro consultara el saldo o el contacto de
 * emergencia de otro: la anotación de rol no mira el id de la ruta. De ahí que la comparación tenga
 * que hacerse aparte.
 * <p>
 * Se registra con el nombre {@code "ownership"} para poder invocarlo desde SpEL; ver
 * {@link Authorities#OR_SELF}.
 */
@Component("ownership")
@RequiredArgsConstructor
public class OwnershipGuard {

    private final EmergencyContactRepository emergencyContactRepository;

    /**
     * @param personId id de PERSONA, no de usuario. Es lo que llevan estas rutas, y es la clave que la
     *                 cuenta guarda en usuario.persona_id.
     * @return false ante cualquier duda —sin autenticación, sin persona asociada, id nulo— porque un
     *         guard que se equivoca abriendo es peor que uno que se equivoca cerrando.
     */
    public boolean isSelf(Authentication authentication, Integer personId) {
        if (personId == null || authentication == null) {
            return false;
        }
        if (!(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return false;
        }

        // Una cuenta ADMIN puede no tener persona detrás. Ahí esto simplemente no aplica, y quien
        // llama pasa (o no) por su rol, que es la otra mitad del OR.
        Person person = details.getUser().getPerson();

        return person != null && person.getId().equals(personId);
    }

    /**
     * Si un contacto de emergencia pertenece a quien lo está editando o borrando.
     * <p>
     * Hace falta porque PUT y DELETE /api/contactos-emergencia/{contactoId} llevan en la ruta el id
     * del CONTACTO, no el de la persona. Usar {@link #isSelf} ahí compararía el id de persona de
     * quien llama contra un id de contacto: números de tablas distintas que coinciden por
     * casualidad. Un miembro podría acabar editando el contacto de otro solo porque los
     * identificadores cuadran.
     *
     * @return false si el contacto no existe. El 404 lo da el service; un guard no debería filtrar
     *         qué ids existen respondiendo distinto.
     */
    public boolean ownsContact(Authentication authentication, Integer contactId) {
        if (contactId == null) {
            return false;
        }

        return emergencyContactRepository.findPersonIdById(contactId)
                .map(personId -> isSelf(authentication, personId))
                .orElse(false);
    }
}
