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
import com.jhanp.psraerobicsbackend.modules.user.dto.response.UserResponse;
import com.jhanp.psraerobicsbackend.modules.user.entity.User;
import com.jhanp.psraerobicsbackend.modules.user.repository.UserRepository;
import com.jhanp.psraerobicsbackend.modules.user.service.interfaces.IUserService;
import com.jhanp.psraerobicsbackend.shared.exception.DuplicateResourceException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * El alta pasa por dos procedimientos en una sola transacción: sp_registrar_persona y, acto seguido,
 * la creación de la cuenta. Si la segunda falla, la persona tampoco queda registrada.
 * <p>
 * userId no cuelga de la entidad Person — la clave foránea vive en usuario — así que lo resuelve
 * este service y se lo pasa al mapper.
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
    private final IUserService userService;

    public PersonService(PersonRepository personRepository,
                         PersonProcedureRepository personProcedureRepository,
                         PersonProfileViewRepository personProfileViewRepository,
                         PersonBalanceViewRepository personBalanceViewRepository,
                         UserRepository userRepository,
                         PersonMapper personMapper,
                         PersonViewMapper personViewMapper,
                         IUserService userService) {
        this.personRepository = personRepository;
        this.personProcedureRepository = personProcedureRepository;
        this.personProfileViewRepository = personProfileViewRepository;
        this.personBalanceViewRepository = personBalanceViewRepository;
        this.userRepository = userRepository;
        this.personMapper = personMapper;
        this.personViewMapper = personViewMapper;
        this.userService = userService;
    }

    /**
     * source lo decide la ruta (ADMIN o AUTOREGISTRO), nunca el cliente.
     * <p>
     * El duplicado se comprueba antes del CALL para responder 409 con un mensaje claro; sin esto, la
     * restricción UNIQUE de la tabla saltaría como un error de base de datos genérico.
     */
    @Override
    @Transactional
    public PersonResponse create(PersonRequest request, RegistrationSource source) {
        if (personRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new DuplicateResourceException(
                    "Ya existe una persona con el documento " + request.documentNumber());
        }

        Integer personId = personProcedureRepository.registerPerson(
                request.documentType(),
                request.documentNumber(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                source);

        UserResponse user = userService.createForPerson(personId, initialPassword(request));

        return personMapper.toResponse(findPersonOrThrow(personId), user.id());
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

        personMapper.updateEntity(request, person);

        return personMapper.toResponse(personRepository.save(person), resolveUserId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse findById(Integer id) {
        return personMapper.toResponse(findPersonOrThrow(id), resolveUserId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse findByDocumentNumber(String documentNumber) {
        Person person = personRepository.findByDocumentNumber(documentNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una persona con el documento " + documentNumber));

        return personMapper.toResponse(person, resolveUserId(person.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonResponse> findAll(Pageable pageable) {
        return toResponsePage(personRepository.findAll(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PersonResponse> search(String term, Pageable pageable) {
        return toResponsePage(personRepository.search(term, pageable));
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

        return personMapper.toResponse(findPersonOrThrow(id), resolveUserId(id));
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
     * RF-01: si no se envía contraseña, la inicial es el propio número de documento.
     */
    private String initialPassword(PersonRequest request) {
        return request.password() != null && !request.password().isBlank()
                ? request.password()
                : request.documentNumber();
    }

    private Person findPersonOrThrow(Integer id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la persona " + id));
    }

    private Integer resolveUserId(Integer personId) {
        return userRepository.findByPersonId(personId)
                .map(User::getId)
                .orElse(null);
    }

    /**
     * Resuelve los userId de la página entera con una sola consulta. Hacerlo persona por persona
     * sería una consulta por fila (N+1).
     */
    private Page<PersonResponse> toResponsePage(Page<Person> page) {
        List<Integer> personIds = page.getContent().stream().map(Person::getId).toList();

        Map<Integer, Integer> userIdByPerson = personIds.isEmpty()
                ? Map.of()
                : userRepository.findByPersonIdIn(personIds).stream()
                .filter(user -> user.getPerson() != null)
                .collect(Collectors.toMap(
                        user -> user.getPerson().getId(),
                        User::getId,
                        (first, second) -> first));

        return page.map(person -> personMapper.toResponse(person, userIdByPerson.get(person.getId())));
    }
}
