package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IPersonService;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquesta las dos formas de dar de alta a una persona, que son endpoints distintos con permisos
 * distintos:
 *
 * <pre>
 *   POST /api/personas               ADMIN     -> lo crea un administrador
 *   POST /api/personas/autoregistro  público   -> lo crea la propia persona en el kiosko
 * </pre>
 *
 * La fuente de registro la fija el método y nunca el cliente. Si viniera en el body, cualquiera
 * podría marcar un registro manual como autoregistro.
 * <p>
 * El resto es idéntico en los dos casos, y por eso comparten el trabajo: misma persona, misma cuenta,
 * misma contraseña inicial y mismo correo. Lo único que cambia es quién aprieta el botón.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterService {

    private final IPersonService personService;
    private final IUserService userService;
    private final IInvitationService invitationService;

    /** Alta hecha por un administrador. */
    @Transactional
    public PersonResponse registerByAdmin(PersonRequest request) {
        log.info("Alta desde admin para el documento {}", request.documentNumber());
        return register(request, RegistrationSource.ADMIN);
    }

    /** Alta que hace la propia persona desde el kiosko. */
    @Transactional
    public PersonResponse registerSelf(PersonRequest request) {
        log.info("Autoregistro para el documento {}", request.documentNumber());
        return register(request, RegistrationSource.SELF_REGISTRATION);
    }

    /**
     * Persona y cuenta en una sola transacción: si la cuenta falla, la persona tampoco queda
     * registrada. Nadie debe existir a medias, sin forma de entrar.
     */
    private PersonResponse register(PersonRequest request, RegistrationSource source) {
        Integer personId = personService.createPerson(request, source);

        UserResponse user = userService.createForPerson(
                personId, initialPassword(request), request.email());

        // Último paso, ya con la cuenta creada: emite el token y publica el evento del correo. El
        // envío ocurre después del commit, así que un SMTP caído no tumba el alta — la persona ya
        // puede entrar con su número de documento.
        //
        // Se le pasa si la contraseña acabó siendo el documento porque el correo de bienvenida lo
        // afirma por escrito. Sin este dato, el correo mentía a quien había elegido una propia.
        invitationService.invite(user.id(), !hasCustomPassword(request));

        log.info("Alta completada: persona {} con cuenta {}", personId, user.id());
        return personService.findById(personId);
    }

    /**
     * RF-01: si no se envía contraseña, la inicial es el propio número de documento.
     * <p>
     * El @Size(min = 8) de PersonRequest solo aplica cuando la contraseña viene en el body; el
     * documento como respaldo no pasa por esa validación, que es justo lo que se busca — hay
     * documentos de menos de 8 dígitos y aun así tienen que poder entrar.
     */
    private String initialPassword(PersonRequest request) {
        return hasCustomPassword(request) ? request.password() : request.documentNumber();
    }

    /**
     * Extraído para que la contraseña que se guarda y lo que el correo dice sobre ella no puedan
     * discrepar: las dos decisiones salen de esta única condición.
     */
    private boolean hasCustomPassword(PersonRequest request) {
        return request.password() != null && !request.password().isBlank();
    }
}
