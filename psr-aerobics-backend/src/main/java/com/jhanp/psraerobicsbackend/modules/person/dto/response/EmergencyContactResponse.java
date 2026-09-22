package com.jhanp.psraerobicsbackend.modules.person.dto.response;

import java.time.OffsetDateTime;

/**
 * Salida de los cuatro endpoints de contactos de emergencia.
 * <p>
 * <b>Ahora lleva id propio, y hace falta.</b> Antes no lo tenía porque la relación con persona era
 * 1:1 y personId ya identificaba la fila; con hasta cinco contactos, personId solo dice a quién
 * pertenecen. Es el id con el que el frontend direcciona
 * PUT/DELETE /api/contactos-emergencia/{contactoId}.
 * <p>
 * personId se conserva para que el cliente sepa de quién es el contacto sin tener que recordar desde
 * qué persona lo pidió.
 */
public record EmergencyContactResponse(
        Integer id,
        Integer personId,
        String name,
        String phone,
        OffsetDateTime updatedAt
) {
}
