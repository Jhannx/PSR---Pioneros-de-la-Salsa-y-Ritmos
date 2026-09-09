package com.jhanp.psraerobicsbackend.modules.person.service;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;
import com.jhanp.psraerobicsbackend.modules.person.entity.EmergencyContact;
import com.jhanp.psraerobicsbackend.modules.person.entity.Person;
import com.jhanp.psraerobicsbackend.modules.person.mapper.EmergencyContactMapper;
import com.jhanp.psraerobicsbackend.modules.person.repository.EmergencyContactRepository;
import com.jhanp.psraerobicsbackend.modules.person.repository.PersonRepository;
import com.jhanp.psraerobicsbackend.modules.person.service.interfaces.IEmergencyContactService;
import com.jhanp.psraerobicsbackend.shared.exception.InvalidDataException;
import com.jhanp.psraerobicsbackend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA plano. La relación con persona es 1:1 (persona_id es UNIQUE), así que el PUT es un upsert
 * manual: se busca por persona y, si no existe la fila, se crea.
 */
@Service
public class EmergencyContactService implements IEmergencyContactService {

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

    @Override
    @Transactional(readOnly = true)
    public EmergencyContactResponse findByPersonId(Integer personId) {
        return emergencyContactRepository.findByPersonId(personId)
                .map(emergencyContactMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La persona " + personId + " no tiene contacto de emergencia registrado"));
    }

    /**
     * La regla de "al menos uno de los tres campos" se comprueba aquí para responder 400 con un
     * mensaje entendible; en la base la respalda chk_contacto_emergencia_no_vacio.
     */
    @Override
    @Transactional
    public EmergencyContactResponse save(Integer personId, EmergencyContactRequest request) {
        if (isEmpty(request)) {
            throw new InvalidDataException(
                    "El contacto de emergencia necesita al menos un nombre, un teléfono o una observación");
        }

        EmergencyContact contact = emergencyContactRepository.findByPersonId(personId)
                .orElseGet(() -> newContactFor(personId));

        emergencyContactMapper.updateEntity(request, contact);

        return emergencyContactMapper.toResponse(emergencyContactRepository.save(contact));
    }

    private EmergencyContact newContactFor(Integer personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la persona " + personId));

        EmergencyContact contact = new EmergencyContact();
        contact.setPerson(person);

        return contact;
    }

    private boolean isEmpty(EmergencyContactRequest request) {
        return isBlank(request.name()) && isBlank(request.phone()) && isBlank(request.observations());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
