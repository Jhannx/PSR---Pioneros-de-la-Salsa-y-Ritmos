package com.jhanp.psraerobicsbackend.modules.person.service;

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

    public EmergencyContactService(EmergencyContactRepository emergencyContactRepository,
                                   PersonRepository personRepository,
                                   EmergencyContactMapper emergencyContactMapper) {
        this.emergencyContactRepository = emergencyContactRepository;
        this.personRepository = personRepository;
        this.emergencyContactMapper = emergencyContactMapper;
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

    @Override
    @Transactional
    public EmergencyContactResponse create(Integer personId, EmergencyContactRequest request) {
        requireNotEmpty(request);

        Person person = requirePerson(personId);

        if (emergencyContactRepository.countByPersonId(personId) >= MAX_CONTACTS_PER_PERSON) {
            throw new ConflictException("La persona ya tiene " + MAX_CONTACTS_PER_PERSON
                    + " contactos de emergencia. Elimina uno antes de agregar otro.");
        }

        EmergencyContact contact = emergencyContactMapper.toEntity(request);
        contact.setPerson(person);

        return emergencyContactMapper.toResponse(emergencyContactRepository.save(contact));
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
