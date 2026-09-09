package com.jhanp.psraerobicsbackend.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ErrorResponse {

    private int status;

    /**
     * Código propio del dominio, para los errores donde el frontend tiene que ramificar y no le
     * basta el status HTTP. Hoy lo usan los dos SQLSTATE del sistema: persona desactivada y no hay
     * clase vigente son ambos 409, pero llevan a acciones distintas en pantalla.
     * <p>
     * Va null (y se omite del JSON) en los errores donde el status ya lo dice todo. Sin él, el
     * frontend tendría que comparar el texto del mensaje para distinguirlos.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String code;

    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String message) {
        this(status, null, message);
    }

    public ErrorResponse(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
