package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IPersonService;
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

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

    /** Mayoría de edad en Colombia. Ver validateIsAdult: es una restricción de la v1, no definitiva. */
    private static final int MINIMUM_AGE = 18;

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
        validateIsAdult(request.birthDate());

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
     * Solo mayores de edad en la v1.
     * <p>
     * No es una regla del negocio —una academia de aeróbicos no tiene por qué excluir a menores— sino
     * de tratamiento de datos: un menor exige consentimiento de quien ejerce la patria potestad, y eso
     * es un flujo entero (quién lo da, cómo se guarda, cómo se revoca) que no existe todavía. Hasta
     * que exista, es más honesto rechazar el alta que registrar a un menor sin el respaldo legal.
     * <p>
     * Va aquí y no en PersonRequest porque este es el único punto por el que pasan las dos altas, la
     * de admin y el autoregistro. Con un @NotNull ya garantizado sobre birthDate no hay forma de
     * esquivarla mandando el campo vacío.
     * <p>
     * Se calcula con Period y no restando años: así el que cumple 18 hoy entra, y el que los cumple
     * mañana no.
     */
    private void validateIsAdult(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < MINIMUM_AGE) {
            log.warn("Alta rechazada por edad: la persona no alcanza los {} años", MINIMUM_AGE);
            throw new InvalidDataException(
                    "Solo pueden registrarse personas mayores de " + MINIMUM_AGE + " años.");
        }
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
