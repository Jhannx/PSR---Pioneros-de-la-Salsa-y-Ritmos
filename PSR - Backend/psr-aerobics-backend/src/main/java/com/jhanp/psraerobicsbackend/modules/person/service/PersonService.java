package com.jhanp.psraerobicsbackend.modules.person.service;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.PersonRequest;
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
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import com.jhanp.psraerobicsbackend.shared.validation.SearchTerms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

    public PersonService(PersonRepository personRepository,
                         PersonProcedureRepository personProcedureRepository,
                         PersonProfileViewRepository personProfileViewRepository,
                         PersonBalanceViewRepository personBalanceViewRepository,
                         UserRepository userRepository,
                         PersonMapper personMapper,
                         PersonViewMapper personViewMapper) {
        this.personRepository = personRepository;
        this.personProcedureRepository = personProcedureRepository;
        this.personProfileViewRepository = personProfileViewRepository;
        this.personBalanceViewRepository = personBalanceViewRepository;
        this.userRepository = userRepository;
        this.personMapper = personMapper;
        this.personViewMapper = personViewMapper;
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
     */
    @Override
    @Transactional
    public PersonResponse updateHealthObservations(Integer id, String healthObservations) {
        Person person = findPersonOrThrow(id);
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

        if (isPhoneTaken(request.phone(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe otra persona con el número de teléfono " + request.phone());
        }

        personMapper.updateEntity(request, person);

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
