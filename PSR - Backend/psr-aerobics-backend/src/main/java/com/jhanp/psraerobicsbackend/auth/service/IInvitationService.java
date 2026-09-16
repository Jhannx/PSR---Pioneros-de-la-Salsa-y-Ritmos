package com.jhanp.psraerobicsbackend.auth.service;

public interface IInvitationService {

    /**
     * Emite el token de invitación de una cuenta recién creada y dispara el correo de bienvenida.
     * <p>
     * No recibe la contraseña, solo si fue la de por defecto: el correo la describe con palabras y
     * nunca con el valor, así que una credencial de menos viajando por aquí es una de menos que se
     * puede acabar en un log.
     * <p>
     * Si la cuenta no tiene correo, no hace nada: es el caso normal del kiosko.
     *
     * @param passwordIsDocument true si la contraseña inicial acabó siendo el número de documento;
     *                           false si la persona eligió una propia al registrarse. El correo de
     *                           bienvenida afirma cuál es, y con false esa frase se omite.
     */
    void invite(Integer userId, boolean passwordIsDocument);
}
