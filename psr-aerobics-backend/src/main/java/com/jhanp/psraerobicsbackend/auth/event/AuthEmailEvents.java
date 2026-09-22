package com.jhanp.psraerobicsbackend.auth.event;

/**
 * Los tres correos que emite auth, como eventos.
 * <p>
 * Llevan datos sueltos y no entidades a propósito: {@link AuthEmailListener} los recibe DESPUÉS del
 * commit, con la sesión de Hibernate ya cerrada, así que cualquier relación perezosa sin resolver
 * reventaría al leerla. Todo lo que el correo necesita se extrae dentro de la transacción.
 */
public final class AuthEmailEvents {

    private AuthEmailEvents() {
    }

    /**
     * Alta: el perfil quedó creado y se invita a fijar una contraseña propia.
     * <p>
     * {@code documentNumber} viaja siempre, porque el correo lo usa para decir con qué se puede
     * entrar —correo o documento— y eso es cierto en los dos casos. Lo que depende de
     * {@code passwordIsDocument} es solo la frase que afirma cuál ES la contraseña: si la persona
     * eligió una propia al registrarse, esa frase sería falsa y se omite.
     */
    public record Welcome(
            String toAddress,
            String toName,
            String documentNumber,
            String token,
            long validityHours,
            boolean passwordIsDocument
    ) {}

    /** "Olvidé mi contraseña": enlace para restablecerla. */
    public record PasswordRecovery(
            String toAddress,
            String toName,
            String token,
            long validityMinutes
    ) {}

    /** Aviso de que la contraseña acaba de cambiar. */
    public record PasswordChanged(
            String toAddress,
            String toName
    ) {}

    /**
     * Confirmación de cambio de correo. {@code toAddress} es el correo NUEVO: el enlace tiene que
     * llegarle a quien dice ser el dueño de ese buzón, que es lo único que este flujo verifica.
     */
    public record EmailChangeRequested(
            String toAddress,
            String toName,
            String token,
            long validityHours
    ) {}
}
