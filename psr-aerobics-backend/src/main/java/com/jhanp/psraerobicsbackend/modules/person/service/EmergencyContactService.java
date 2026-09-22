package com.jhanp.psraerobicsbackend.modules.person.service;

import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentMedium;
import com.jhanp.psraerobicsbackend.modules.consent.enums.ConsentType;
import com.jhanp.psraerobicsbackend.modules.consent.service.interfaces.IConsentService;
import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;
import com.jhanp.psraerobicsbackend.modules.person.entity.EmergencyContact;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.person.mapper.EmergencyContactMapper;
import com.jhanp.psraerobicsbackend.modules.person.repository.EmergencyContactRepository;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonRepository;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IEmergencyContactService;
import com.jhanp.psraerobicsbackend.shared.exception.ConflictException;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA plano. Hasta cinco contactos por persona, sin orden de prioridad.
 * <p>
 * El tope se comprueba aquí Y en la base (trg_limitar_contactos_emergencia). No es duplicación
 * gratuita: el trigger es lo que garantiza el dato pase lo que pase —incluido un INSERT a mano por
 * SQL—, pero su RAISE EXCEPTION llega al cliente como error de base de datos. El mensaje que se
 * puede leer lo da este service. Mismo reparto que la validación de día en las excepciones de
 * horario.
 */
@Service
public class EmergencyContactService implements IEmergencyContactService {

    /** Sincronizado con trg_limitar_contactos_emergencia. Si cambia uno, cambia el otro. */
    private static final int MAX_CONTACTS_PER_PERSON = 5;

    private final EmergencyContactRepository emergencyContactRepository;
    private final PersonRepository personRepository;
    private final EmergencyContactMapper emergencyContactMapper;

    /** Para la declaración del numeral 5, que es lo único que este service tiene que acreditar. */
    private final IConsentService consentService;

    public EmergencyContactService(EmergencyContactRepository emergencyContactRepository,
                                   PersonRepository personRepository,
                                   EmergencyContactMapper emergencyContactMapper,
                                   IConsentService consentService) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.personRepository = personRepository;
        this.emergencyContactMapper = emergencyContactMapper;
        this.consentService = consentService;
    }

    /**
     * Lista vacía y no 404 cuando la persona no tiene ninguno: "sin contactos" es un estado normal,
     * no un error. Lo que sí es 404 es preguntar por una persona que no existe — si no, un id
     * inventado devolvería [] y parecería una ficha vacía.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> findByPersonId(Integer personId) {
        requirePerson(personId);

        return emergencyContactRepository.findByPersonIdOrderByIdAsc(personId)
                .stream()
                .map(emergencyContactMapper::toResponse)
                .toList();
    }

    /**
     * <b>Una fila de consentimiento por contacto, no una por persona.</b>
     * <p>
     * Es la diferencia con el dato de salud, donde la autorización solo se graba al cambiar de
     * estado. Allí la persona autoriza el tratamiento de <em>sus</em> datos una vez y esa decisión
     * vale para todas las ediciones; aquí cada contacto es <em>un tercero distinto</em>, y la
     * declaración del numeral 5 dice "estoy autorizado a suministrar estos datos" — estos, los de esa
     * persona concreta. Dar por buena para un contacto la declaración hecha sobre otro sería afirmar
     * algo de alguien de quien nunca se dijo nada.
     * <p>
     * El medio lo pone quien llama: por el endpoint es la casilla de la pantalla (DIGITAL), y desde el
     * alta de mostrador es el formato firmado (PHYSICAL). Asumir DIGITAL dejaría constancia de una
     * casilla que nadie marcó.
     */
    @Override
    @Transactional
    public EmergencyContactResponse create(Integer personId,
                                           EmergencyContactRequest request,
                                           ConsentMedium medium,
                                           Integer actingUserId) {
        requireNotEmpty(request);
        requireThirdPartyConsent(request);

        Person person = requirePerson(personId);

        if (emergencyContactRepository.countByPersonId(personId) >= MAX_CONTACTS_PER_PERSON) {
            throw new ConflictException("La persona ya tiene " + MAX_CONTACTS_PER_PERSON
                    + " contactos de emergencia. Elimina uno antes de agregar otro.");
        }

        EmergencyContact contact = emergencyContactMapper.toEntity(request);
        contact.setPerson(person);

        EmergencyContactResponse response =
                emergencyContactMapper.toResponse(emergencyContactRepository.save(contact));

        // Después del save y en la misma transacción: si guardar el contacto falla, no queda una
        // declaración sobre unos datos que nunca llegaron a existir.
        consentService.record(personId, ConsentType.THIRD_PARTY_DATA, true,
                medium, actingUserId);

        return response;
    }

    @Override
    @Transactional
    public EmergencyContactResponse update(Integer contactId, EmergencyContactRequest request) {
        requireNotEmpty(request);

        EmergencyContact contact = requireContact(contactId);
        emergencyContactMapper.updateEntity(request, contact);

        return emergencyContactMapper.toResponse(emergencyContactRepository.save(contact));
    }

    /**
     * Sin mínimo: una persona puede quedarse sin ningún contacto. Es un dato deseable, no
     * obligatorio, y bloquear el borrado del último obligaría a inventarse uno para poder corregirlo.
     */
    @Override
    @Transactional
    public void delete(Integer contactId) {
        emergencyContactRepository.delete(requireContact(contactId));
    }

    private Person requirePerson(Integer personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la persona " + personId));
    }

    private EmergencyContact requireContact(Integer contactId) {
        return emergencyContactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el contacto de emergencia " + contactId));
    }

    /**
     * Dos campos y no tres: las observaciones se fueron a la persona. Lo respalda
     * chk_contacto_emergencia_no_vacio; aquí se comprueba para dar un 400 entendible.
     */
    private void requireNotEmpty(EmergencyContactRequest request) {
        if (isBlank(request.name()) && isBlank(request.phone())) {
            throw new InvalidDataException(
                    "El contacto de emergencia necesita al menos un nombre o un teléfono");
        }
    }

    /**
     * Numeral 5 de la política. Solo al crear: editar un contacto que ya existe no suministra datos
     * de nadie nuevo.
     * <p>
     * Se comprueba en el servidor aunque el frontend ya bloquee el botón, por lo mismo que el resto
     * de consentimientos: ese candado es de interfaz y se salta llamando a la API directamente, así
     * que no sirve como prueba de nada.
     */
    private void requireThirdPartyConsent(EmergencyContactRequest request) {
        if (!Boolean.TRUE.equals(request.thirdPartyConsent())) {
            throw new InvalidDataException(
                    "Para registrar un contacto de emergencia hay que declarar que estás autorizado "
                            + "a suministrar sus datos para esta finalidad.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
