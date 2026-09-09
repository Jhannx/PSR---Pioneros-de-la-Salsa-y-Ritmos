package com.jhanp.psraerobicsbackend.shared.exception;

public class EmailException extends RuntimeException {
    public EmailException(String message) {
        super(message);
    }

    /**
     * La causa importa: el fallo real (host inalcanzable, credenciales, 400 de la API) viene de la
     * librería de transporte, y sin encadenarla el log solo muestra el mensaje genérico.
     */
    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
