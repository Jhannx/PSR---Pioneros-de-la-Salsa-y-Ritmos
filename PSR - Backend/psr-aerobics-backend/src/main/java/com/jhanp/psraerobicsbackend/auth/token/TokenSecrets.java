package com.jhanp.psraerobicsbackend.auth.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cómo se fabrica y cómo se hashea un token de un solo uso. Nada más: no sabe de tablas, de correos
 * ni de caducidades.
 * <p>
 * Vive aparte porque ya son dos los flujos que emiten tokens —contraseña y cambio de correo— y la
 * parte criptográfica tiene que ser la misma en los dos. Duplicarla significaría que el día que se
 * cambie el tamaño del token o el algoritmo, uno de los dos se quede atrás sin que nadie lo note.
 */
public final class TokenSecrets {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32; // 256 bits

    private TokenSecrets() {
    }

    /**
     * Un token nuevo, en claro. Es la única vez que existe fuera del buzón de la persona: quien lo
     * reciba debe meterlo en un correo y olvidarlo, nunca guardarlo ni escribirlo en un log.
     */
    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        // URL-safe y sin '=': el token viaja dentro de un enlace, no puede llevar +, / ni =.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, no BCrypt, y la diferencia importa.
     * <p>
     * A las contraseñas se les aplica BCrypt porque son cortas y predecibles: hace falta un algoritmo
     * lento y con sal para que probar millones de candidatas salga caro. Un token tiene 256 bits de
     * aleatoriedad: no hay diccionario que lo contenga ni fuerza bruta viable, así que no se gana nada
     * con un algoritmo lento.
     * <p>
     * Y sobre todo: BCrypt genera una sal distinta cada vez, así que el mismo token produciría hashes
     * distintos y no se podría BUSCAR por él. SHA-256 es determinista, y eso permite indexar la columna
     * y resolver el canje en una sola consulta.
     */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM; si falta, el entorno está roto.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}
