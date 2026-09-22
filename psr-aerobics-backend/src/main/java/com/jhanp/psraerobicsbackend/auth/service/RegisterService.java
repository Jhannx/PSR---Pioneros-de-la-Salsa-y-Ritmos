package com.jhanp.psraerobicsbackend.auth.service;

import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;
import com.jhanp.psraerobicsbackend.modules.consent.service.interfaces.IConsentService;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IEmergencyContactService;
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
 *   POST /api/personas               ADMIN     -&gt; lo crea un administrador
 *   POST /api/personas/autoregistro  público   -&gt; lo crea la propia persona en el kiosko
 * </pre>
 *
 * La fuente de registro la fija el método y nunca el cliente. Si viniera en el body, cualquiera
 * podría marcar un registro manual como autoregistro.
 * <p>
 * El resto es casi idéntico en los dos casos, y por eso comparten el trabajo: misma persona, misma
 * cuenta, misma contraseña inicial y mismo correo. Lo que cambia, además de quién aprieta el botón,
 * es <b>cómo se autorizó el tratamiento de datos</b> — en pantalla o en papel —, y eso queda
 * registrado en cada alta.
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

    /**
     * Para los contactos que puedan venir en el formulario de registro. Se llama al service y no al
     * endpoint: el {@code @PreAuthorize} vive en el controller, y quien se autoregistra no tiene
     * sesión con la que atravesarlo.
     */
    private final IEmergencyContactService emergencyContactService;

    /**
     * El registro probatorio del artículo 9 de la Ley 1581. Sin esto, el alta guardaba los datos de
     * la persona sin guardar en ningún sitio que hubiera autorizado a guardarlos.
     */
    private final IConsentService consentService;

    /**
     * Alta hecha por un administrador. La autorización es el formato físico firmado, no una casilla.
     *
     * @param registeredByUserId la cuenta de quien registra, que queda como autor de la constancia
     */
    @Transactional
    public PersonResponse registerByAdmin(PersonRequest request, Integer registeredByUserId) {
        log.info("Alta desde admin para el documento {}", request.documentNumber());
        return register(request, RegistrationSource.ADMIN, registeredByUserId);
    }

    /**
     * Alta que hace la propia persona desde el kiosko.
     * <p>
     * Sin id de usuario: la ruta es pública y no hay sesión. La constancia queda con registrado_por
     * en null, que es lo que significa "lo marcó ella misma, sin intermediario".
     */
    @Transactional
    public PersonResponse registerSelf(PersonRequest request) {
        log.info("Autoregistro para el documento {}", request.documentNumber());
        return register(request, RegistrationSource.SELF_REGISTRATION, null);
    }

    /**
     * Persona, constancia de consentimiento y cuenta en una sola transacción: si cualquiera de las
     * tres falla, no queda ninguna de las otras. Nadie debe existir a medias, sin forma de entrar — ni
     * registrado sin que conste que autorizó.
     */
    private PersonResponse register(PersonRequest request,
                                    RegistrationSource source,
                                    Integer registeredByUserId) {
        validateIsAdult(request.birthDate());
        validateConsent(request, source);

        Integer personId = personService.createPerson(request, source);

        recordRegistrationConsent(personId, source, registeredByUserId);
        recordOptionalData(request, personId, source, registeredByUserId);

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
     * Sin autorización no hay alta, y la comprobación es del servidor.
     * <p>
     * <b>Esto no duplica la casilla del frontend: la sustituye como regla.</b> El candado de la
     * interfaz es una ayuda para quien usa la app, no una prueba de nada — se salta con una petición
     * directa a la API, y cualquiera puede hacerla. La única validación que cuenta es esta.
     * <p>
     * Va aquí y no en {@code PersonRequest} porque cada ruta exige campos distintos, y una anotación
     * sobre el record no puede distinguir por dónde entró la petición. Mismo reparto que
     * {@link #validateIsAdult}.
     */
    private void validateConsent(PersonRequest request, RegistrationSource source) {
        if (source == RegistrationSource.ADMIN) {
            if (!Boolean.TRUE.equals(request.signedPhysicalConsent())) {
                log.warn("Alta de mostrador rechazada: sin constancia del formato físico firmado");
                throw new InvalidDataException(
                        "Para registrar a una persona desde el mostrador hay que confirmar que se "
                                + "recogió el formato físico de autorización firmado.");
            }
            return;
        }

        // Los dos por separado y con mensajes distintos: son dos casillas distintas en pantalla, y
        // un mensaje genérico dejaría a quien se registra sin saber cuál le falta.
        if (!Boolean.TRUE.equals(request.dataProcessingConsent())) {
            log.warn("Autoregistro rechazado: sin autorización de tratamiento de datos");
            throw new InvalidDataException(
                    "Hay que autorizar el tratamiento de datos personales para poder registrarse.");
        }

        if (!Boolean.TRUE.equals(request.voluntaryParticipationConsent())) {
            log.warn("Autoregistro rechazado: sin declaración de participación voluntaria");
            throw new InvalidDataException(
                    "Hay que aceptar la declaración de participación voluntaria para poder registrarse.");
        }
    }

    /**
     * Dos filas por alta, siempre: el tratamiento de datos (numeral 4.1) y la participación
     * voluntaria (numeral 12).
     * <p>
     * Dos y no una aunque en el alta de mostrador las respalde el mismo papel, porque son
     * autorizaciones independientes: la persona puede revocar una sin revocar la otra, y una sola
     * fila no podría representar ese estado.
     * <p>
     * El medio sale de la fuente y no del body — igual que la propia fuente. Si el cliente pudiera
     * elegirlo, un autoregistro podría declararse respaldado por un papel que no existe.
     */
    private void recordRegistrationConsent(Integer personId,
                                           RegistrationSource source,
                                           Integer registeredByUserId) {
        ConsentMedium medium = source == RegistrationSource.ADMIN
                ? ConsentMedium.PHYSICAL
                : ConsentMedium.DIGITAL;

        consentService.record(personId, ConsentType.DATA_PROCESSING, true, medium, registeredByUserId);
        consentService.record(personId, ConsentType.VOLUNTARY_PARTICIPATION, true, medium, registeredByUserId);
    }

    /**
     * Los datos opcionales que el formulario de registro puede traer: observaciones de salud y
     * contactos de emergencia.
     * <p>
     * <b>Por qué viven en el alta y no en sus propios endpoints.</b> Tres de las cuatro pantallas de
     * registro son públicas —autoregistro, kiosko del lobby, alta de miembro—, así que quien las usa
     * no tiene token todavía; {@code POST /{id}/contactos-emergencia} y
     * {@code PUT /{id}/observaciones-salud} exigen sesión, y encadenarlos después del alta es
     * imposible porque no hay con qué autenticarlos. Este método corre con permisos de servidor y
     * dentro de la misma transacción, que es lo que hace viable el caso.
     * <p>
     * <b>No se reimplementa ninguna regla.</b> Se llama a los mismos servicios que atienden esos
     * endpoints, así que el tope de cinco contactos, el "al menos nombre o teléfono", la exigencia de
     * la declaración del numeral 5 en cada contacto y la del numeral 4.2 cuando hay contenido de salud
     * se aplican aquí igual, con los mismos mensajes. El {@code @PreAuthorize} que falta no es una
     * puerta que se esquive: vive en los controllers, y la autorización de esta ruta ya se decidió
     * arriba.
     * <p>
     * Todo dentro de la transacción del alta: si un contacto viene sin su declaración, no queda la
     * persona registrada a medias con la mitad de sus contactos — no queda nada.
     */
    private void recordOptionalData(PersonRequest request,
                                    Integer personId,
                                    RegistrationSource source,
                                    Integer registeredByUserId) {

        // El mismo medio que la autorización general del alta, y por el mismo motivo: lo que respalda
        // un alta de mostrador es el papel firmado, no una casilla que allí nadie marca.
        ConsentMedium medium = source == RegistrationSource.ADMIN
                ? ConsentMedium.PHYSICAL
                : ConsentMedium.DIGITAL;

        if (request.healthObservations() != null && !request.healthObservations().isBlank()) {
            personService.updateHealthObservations(personId, request.healthObservations(),
                    request.healthDataConsent(), medium, registeredByUserId);
        }

        if (request.emergencyContacts() != null) {
            for (EmergencyContactRequest contact : request.emergencyContacts()) {
                emergencyContactService.create(personId, contact, medium, registeredByUserId);
            }
        }
    }

    /**
     * Solo mayores de edad en la v1.
     * <p>
     * No es una regla del negocio —una academia de aeróbicos no tiene por qué excluir a menores— sino
     * de tratamiento de datos: un menor exige consentimiento de quien ejerce la patria potestad, y eso
     * es un flujo entero (quién lo da, cómo se guarda, cómo se revoca) que no existe todavía. Hasta
     * que exista, es más honesto rechazar el alta que registrar a un menor sin el respaldo legal.
     * Lo declara también el numeral 11 de la política.
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
