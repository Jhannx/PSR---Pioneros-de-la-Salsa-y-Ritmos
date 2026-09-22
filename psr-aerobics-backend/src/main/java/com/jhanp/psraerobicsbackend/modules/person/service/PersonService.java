package com.jhanp.psraerobicsbackend.modules.person.service;

import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;
import com.jhanp.psraerobicsbackend.modules.consent.service.interfaces.IConsentService;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonSelfUpdateRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonUpdateRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonBalanceViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonProfileViewResponse;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.PersonResponse;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.person.enums.RegistrationSource;
import com.jhanp.psraerobicsbackend.modules.person.mapper.PersonMapper;
import com.jhanp.psraerobicsbackend.modules.person.mapper.PersonViewMapper;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonBalanceViewRepository;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonProcedureRepository;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonProfileViewRepository;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonRepository;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IPersonService;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.shared.exception.DuplicateResourceException;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.validation.SearchTerms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Todo lo que le pasa a una persona menos su alta completa: consulta, edición, baja, reactivación y
 * las vistas de perfil y saldo.
 * <p>
 * El alta la coordina {@code RegisterService}, que encadena {@link #createPerson} con la creación de
 * la cuenta en una sola transacción. Aquí no se sabe nada de contraseñas ni de correos.
 * <p>
 * userId no cuelga de la entidad Person — la clave foránea vive en usuario — así que lo resuelve este
 * service y se lo pasa al mapper.
 */
@Service
public class PersonService implements IPersonService {

    private final PersonRepository personRepository;
    private final PersonProcedureRepository personProcedureRepository;
    private final PersonProfileViewRepository personProfileViewRepository;
    private final PersonBalanceViewRepository personBalanceViewRepository;
    private final UserRepository userRepository;
    private final PersonMapper personMapper;
    private final PersonViewMapper personViewMapper;

    /**
     * Solo para las observaciones de salud, que son el único dato sensible que toca este service.
     * El resto de campos de persona se rigen por la autorización general que ya se grabó en el alta.
     */
    private final IConsentService consentService;

    public PersonService(PersonRepository personRepository,
                         PersonProcedureRepository personProcedureRepository,
                         PersonProfileViewRepository personProfileViewRepository,
                         PersonBalanceViewRepository personBalanceViewRepository,
                         UserRepository userRepository,
                         PersonMapper personMapper,
                         PersonViewMapper personViewMapper,
                         IConsentService consentService) {
        this.personRepository = personRepository;
        this.personProcedureRepository = personProcedureRepository;
        this.personProfileViewRepository = personProfileViewRepository;
        this.personBalanceViewRepository = personBalanceViewRepository;
        this.userRepository = userRepository;
        this.personMapper = personMapper;
        this.personViewMapper = personViewMapper;
        this.consentService = consentService;
    }

    /**
     * source lo decide la ruta (ADMIN o AUTOREGISTRO), nunca el cliente.
     * <p>
     * El duplicado se comprueba antes del CALL para responder 409 con un mensaje claro; sin esto, la
     * restricción UNIQUE de la tabla saltaría como un error de base de datos genérico.
     * <p>
     * Solo crea la persona: la cuenta la crea {@code RegisterService} acto seguido, en la misma
     * transacción.
     */
    @Override
    @Transactional
    public Integer createPerson(PersonRequest request, RegistrationSource source) {
        if (personRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new DuplicateResourceException(
                    "Ya existe una persona con el documento " + request.documentNumber());
        }

        if (isPhoneTaken(request.phone(), null)) {
            throw new DuplicateResourceException(
                    "Ya existe una persona con el número de teléfono " + request.phone());
        }

        return personProcedureRepository.registerPerson(
                request.documentType(),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.birthDate(),
                source);
    }

    /**
     * Endpoint propio y no parte del PUT de persona, y la razón es de permisos.
     * <p>
     * PUT /api/personas/{id} está restringido al mostrador y reescribe documento, nombre y teléfono.
     * Para que un miembro pudiera editar sus propias observaciones de salud habría que abrir ese PUT
     * a OR_SELF, y entonces también podría cambiarse el número de documento. Un endpoint aparte, que
     * solo toca esta columna, se puede abrir al dueño sin arrastrar nada más.
     * <p>
     * Admite null y blanco: borrar lo que uno escribió es una operación legítima.
     * <p>
     * <b>Aquí vive la regla del numeral 4.2, y es asimétrica a propósito.</b> Los datos de salud son
     * sensibles (artículo 5 de la Ley 1581): escribirlos exige consentimiento expreso, y por eso una
     * petición con contenido y sin la casilla se rechaza con 400 aunque el frontend ya la exija — ese
     * candado es de interfaz y se salta llamando a la API directamente.
     * <p>
     * Borrar, en cambio, no exige nada: pedir permiso de tratamiento para <em>dejar</em> de tratar un
     * dato sería absurdo, y el propio numeral 4.2 dice que no suministrarlo no puede costarle nada a
     * la persona. Vaciar el campo revoca además el consentimiento que hubiera, porque retirar el dato
     * y retirar el permiso son el mismo gesto.
     * <p>
     * <b>El medio viene de quien llama y no se asume DIGITAL.</b> Por este método pasan dos caminos
     * con respaldos distintos: la pantalla, donde la persona marca una casilla, y el alta de mostrador
     * dentro de {@code RegisterService}, donde lo que autoriza es el formato en papel firmado. Grabar
     * "DIGITAL" en el segundo caso sería declarar una casilla que nadie marcó, y la tabla de
     * consentimientos vale justo lo que vale su veracidad.
     */
    @Override
    @Transactional
    public PersonResponse updateHealthObservations(Integer id,
                                                   String healthObservations,
                                                   Boolean healthDataConsent,
                                                   ConsentMedium medium,
                                                   Integer actingUserId) {
        Person person = findPersonOrThrow(id);

        if (healthObservations == null || healthObservations.isBlank()) {
            // Solo si lo había: si nunca autorizó, una fila de revocación no diría nada cierto.
            if (consentService.hasActive(id, ConsentType.HEALTH_DATA)) {
                consentService.record(id, ConsentType.HEALTH_DATA, false,
                        medium, actingUserId);
            }
        } else {
            if (!Boolean.TRUE.equals(healthDataConsent)) {
                throw new InvalidDataException(
                        "Para guardar observaciones de salud hay que autorizar expresamente el "
                                + "tratamiento de este dato. Suministrarlo es opcional: puedes dejar "
                                + "el campo vacío.");
            }

            // Solo en la transición, no en cada guardado. La autorización es "trata mis datos de
            // salud", no "trata esta frase": re-registrarla en cada corrección de una coma llenaría
            // la tabla de filas idénticas y enterraría las que sí marcan un cambio de decisión.
            if (!consentService.hasActive(id, ConsentType.HEALTH_DATA)) {
                consentService.record(id, ConsentType.HEALTH_DATA, true,
                        medium, actingUserId);
            }
        }

        person.setHealthObservations(healthObservations);

        return toResponse(personRepository.save(person));
    }

    @Override
    @Transactional
    public PersonResponse update(Integer id, PersonUpdateRequest request) {
        Person person = findPersonOrThrow(id);

        boolean documentChanged = !request.documentNumber().equals(person.getDocumentNumber());
        if (documentChanged && personRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new DuplicateResourceException(
                    "Ya existe otra persona con el documento " + request.documentNumber());
        }

        // La misma guarda que el documento, y por la misma razón: el PUT es un reemplazo completo, así
        // que el cliente reenvía el teléfono actual tal cual aunque haya editado otro campo. Sin
        // comparar antes, editar la fecha de nacimiento de alguien que comparte número con otra ficha
        // respondía 409 hablando de un teléfono que nadie había tocado.
        boolean phoneChanged = !Objects.equals(request.phone(), person.getPhone());
        if (phoneChanged && isPhoneTaken(request.phone(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe otra persona con el número de teléfono " + request.phone());
        }

        personMapper.updateEntity(request, person);

        return toResponse(personRepository.save(person));
    }

    /**
     * Los datos que la propia persona puede corregir de sí misma: nombres, teléfono y fecha de
     * nacimiento.
     * <p>
     * Va aparte de {@link #update} por lo mismo que las observaciones de salud: aquel reescribe también
     * el documento, y abrirlo a OR_SELF dejaría a cualquiera cambiarse el número con el que se
     * identifica. Aquí el documento no está en el DTO, así que no hay forma de tocarlo — ni por
     * descuido ni a propósito.
     */
    @Override
    @Transactional
    public PersonResponse updateOwnData(Integer id, PersonSelfUpdateRequest request) {
        Person person = findPersonOrThrow(id);

        boolean phoneChanged = !Objects.equals(request.phone(), person.getPhone());
        if (phoneChanged && isPhoneTaken(request.phone(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe otra persona con el número de teléfono " + request.phone());
        }

        personMapper.updateOwnData(request, person);

        return toResponse(personRepository.save(person));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse findById(Integer id) {
        return toResponse(findPersonOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse findByDocumentNumber(String documentNumber) {
        Person person = personRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una persona con el documento " + documentNumber));

        return toResponse(person);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonResponse> findAll(Pageable pageable) {
        return toResponsePage(personRepository.findAll(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonResponse> search(String term, Pageable pageable) {
        // unsorted() no es opcional: la consulta es nativa y trae su propio ORDER BY. Con un
        // ?sort= en la URL, Spring le pega un segundo ORDER BY y el SQL revienta con 500.
        return toResponsePage(personRepository.search(term, SearchTerms.unsorted(pageable)));
    }

    /**
     * Desactivar es JPA plano: apaga la persona y el trigger de sincronización apaga su cuenta.
     * Reactivar, en cambio, va por procedimiento, porque queda auditado.
     */
    @Override
    @Transactional
    public void deactivate(Integer id) {
        Person person = findPersonOrThrow(id);
        person.setActive(false);

        personRepository.save(person);
    }

    @Override
    @Transactional
    public PersonResponse reactivate(Integer id, Integer executedByUserId) {
        personProcedureRepository.reactivate(id, executedByUserId);

        return toResponse(findPersonOrThrow(id));
    }

    /**
     * Se comprueba la existencia antes del CALL para devolver 404 en vez de dejar que el
     * procedimiento lance su propia excepción, que llegaría como error de base de datos.
     */
    @Override
    @Transactional
    public void deletePermanently(Integer id, Integer executedByUserId) {
        if (!personRepository.existsById(id)) {
            throw new ResourceNotFoundException("No existe la persona " + id);
        }

        personProcedureRepository.deletePermanently(id, executedByUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonProfileViewResponse getProfile(Integer id) {
        return personProfileViewRepository.findByPersonId(id)
                .map(personViewMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la persona " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonProfileViewResponse getProfileByDocumentNumber(String documentNumber) {
        return personProfileViewRepository.findByDocumentNumber(documentNumber)
                .map(personViewMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una persona con el documento " + documentNumber));
    }

    /**
     * vista_saldo_persona solo incluye personas activas: una persona desactivada existe pero no
     * tiene fila aquí, y por eso se distingue en el mensaje.
     */
    @Override
    @Transactional(readOnly = true)
    public PersonBalanceViewResponse getBalance(Integer id) {
        return personBalanceViewRepository.findByPersonId(id)
                .map(personViewMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        personRepository.existsById(id)
                                ? "La persona " + id + " está desactivada y no tiene saldo consultable"
                                : "No existe la persona " + id));
    }

    /**
     * El teléfono se trata como el documento y el correo: si ya está, se avisa con un 409 y un mensaje
     * que dice cuál es el número repetido. No es una integridad que garantice la base —no hay UNIQUE
     * en persona.telefono—, es un aviso para que quien está en el mostrador decida: casi siempre
     * significa que esa persona ya estaba registrada, y cuando de verdad comparten número (dos
     * personas de la misma casa) basta con dejarlo en la otra ficha o registrar el suyo propio.
     *
     * @param excludeId persona a excluir de la comprobación, o null al dar de alta
     */
    private boolean isPhoneTaken(String phone, Integer excludeId) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return excludeId == null
                ? personRepository.existsByPhone(phone)
                : personRepository.existsByPhoneAndIdNot(phone, excludeId);
    }

    private Person findPersonOrThrow(Integer id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la persona " + id));
    }

    /**
     * Arma el Response resolviendo la cuenta de esa persona. Una sola consulta para las dos cosas que
     * el Response necesita de usuario: el id y el correo.
     * <p>
     * La cuenta puede no existir: sp_eliminar_persona deja huérfana a la persona sentinel, y nada
     * impide en el esquema que una fila de persona se quede sin usuario.
     */
    private PersonResponse toResponse(Person person) {
        return userRepository.findByPersonId(person.getId())
                .map(user -> personMapper.toResponse(person, user.getId(), user.getEmail()))
                .orElseGet(() -> personMapper.toResponse(person, null, null));
    }

    /**
     * Resuelve las cuentas de la página entera con una sola consulta. Hacerlo persona por persona
     * sería una consulta por fila (N+1).
     */
    private Page<PersonResponse> toResponsePage(Page<Person> page) {
        List<Integer> personIds = page.getContent().stream().map(Person::getId).toList();

        Map<Integer, User> accountByPerson = personIds.isEmpty()
                ? Map.of()
                : userRepository.findByPersonIdIn(personIds).stream()
                .filter(user -> user.getPerson() != null)
                .collect(Collectors.toMap(
                        user -> user.getPerson().getId(),
                        user -> user,
                        (first, second) -> first));

        return page.map(person -> {
            User account = accountByPerson.get(person.getId());
            return account == null
                    ? personMapper.toResponse(person, null, null)
                    : personMapper.toResponse(person, account.getId(), account.getEmail());
        });
    }
}
