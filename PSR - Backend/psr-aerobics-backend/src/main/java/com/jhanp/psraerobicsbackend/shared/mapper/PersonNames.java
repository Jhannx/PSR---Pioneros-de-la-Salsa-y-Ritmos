package com.jhanp.psraerobicsbackend.shared.mapper;

import com.jhanp.psraerobicsbackend.modules.person.entity.Person;

/**
 * Arma el nombre completo que varios Response exponen como un solo campo (fullName, markedByName,
 * updatedByName). Las vistas SQL ya lo resuelven con un CONCAT propio; esto es para los Response
 * que se construyen desde entidades.
 * <p>
 * Devuelve null si no hay persona: un usuario ADMIN puede no tener persona asociada, y una persona
 * eliminada se reasigna al registro sentinel.
 */
public final class PersonNames {

    private PersonNames() {
    }

    public static String fullName(Person person) {
        if (person == null) {
            return null;
        }
        String first = person.getName() != null ? person.getName() : "";
        String last = person.getLastName() != null ? person.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }
}
