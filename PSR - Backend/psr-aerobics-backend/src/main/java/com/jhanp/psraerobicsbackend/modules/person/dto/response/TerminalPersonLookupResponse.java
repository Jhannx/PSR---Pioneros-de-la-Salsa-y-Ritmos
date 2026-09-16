package com.jhanp.psraerobicsbackend.modules.person.dto.response;

/** Datos mínimos para confirmar un documento desde la terminal, sin datos contables ni de cuenta. */
public record TerminalPersonLookupResponse(
        Integer id,
        String documentNumber,
        String firstName,
        String lastName,
        Boolean active
) {
}