package com.jhanp.psraerobicsbackend.modules.person.repository;

import com.jhanp.psraerobicsbackend.modules.person.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Integer> {

    /**
     * Los contactos de una persona, ordenados por id.
     * <p>
     * El orden es fijo y no cosmético: sin ORDER BY, Postgres puede devolver las filas en distinto
     * orden en cada consulta, y la lista de la pantalla bailaría entre recargas. Se ordena por id
     * —orden de creación— y no por nombre, porque todos los contactos tienen el mismo peso y el
     * único orden con significado es en cuál se registró primero.
     */
    List<EmergencyContact> findByPersonIdOrderByIdAsc(Integer personId);

    /** Para el tope de 5, antes de insertar. El trigger lo garantiza; esto da el 409 legible. */
    long countByPersonId(Integer personId);

    /**
     * De qué persona es un contacto. La usa OwnershipGuard para resolver si quien edita o borra un
     * contacto es su dueño.
     * <p>
     * Devuelve el id y no la entidad a propósito: se llama desde un @PreAuthorize, fuera de la
     * transacción del service, y {@code contacto.getPerson().getId()} dependería de que el proxy
     * perezoso resuelva el identificador sin sesión abierta. Pedir solo el id es una consulta y
     * ninguna sorpresa.
     */
    @Query("SELECT c.person.id FROM EmergencyContact c WHERE c.id = :contactId")
    Optional<Integer> findPersonIdById(@Param("contactId") Integer contactId);
}
