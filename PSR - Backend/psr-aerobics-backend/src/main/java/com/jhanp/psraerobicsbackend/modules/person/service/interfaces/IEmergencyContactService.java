package com.jhanp.psraerobicsbackend.modules.person.service.interfaces;

import com.jhanp.psraerobicsbackend.modules.person.dto.request.EmergencyContactRequest;
import com.jhanp.psraerobicsbackend.modules.person.dto.response.EmergencyContactResponse;

import java.util.List;

/**
 * Hasta cinco contactos por persona, todos con el mismo peso: no hay contacto "principal" ni orden de
 * prioridad. Quien atiende una urgencia llama por la lista, no por un ranking que nadie mantiene.
 * <p>
 * Antes era 1:1 y esta interfaz tenía dos métodos —leer el contacto y guardarlo con un upsert—.
 * Ahora son cuatro, y la diferencia de forma importa: crear y editar dejan de ser la misma operación,
 * porque ya no basta con saber de quién es el contacto para saber cuál es.
 */
public interface IEmergencyContactService {

    /** Los contactos de una persona, en orden de creación. Lista vacía si no tiene ninguno. */
    List<EmergencyContactResponse> findByPersonId(Integer personId);

    /**
     * personId va como parámetro porque viaja en el path: un contacto pertenece a la persona desde la
     * que se crea y nunca cambia de dueño.
     *
     * @throws com.jhanp.psraerobicsbackend.shared.exception.ConflictException si ya tiene cinco
     */
    EmergencyContactResponse create(Integer personId, EmergencyContactRequest request);

    /** Edita nombre y teléfono. No puede mover el contacto a otra persona. */
    EmergencyContactResponse update(Integer contactId, EmergencyContactRequest request);

    void delete(Integer contactId);
}
