package com.jhanp.psraerobicsbackend.modules.person.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;

public interface IEmergencyContactService {

    EmergencyContactResponse findByPersonId(Integer personId);

    /**
     * Relación 1:1 con persona (persona_id es UNIQUE): crea el contacto si no existe, o lo actualiza.
     * personId va como parámetro porque viaja en el path, no en el body.
     */
    EmergencyContactResponse save(Integer personId, EmergencyContactRequest request);
}
