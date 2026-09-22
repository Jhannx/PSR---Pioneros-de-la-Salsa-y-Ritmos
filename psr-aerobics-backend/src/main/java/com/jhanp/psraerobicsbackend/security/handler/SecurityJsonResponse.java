package com.jhanp.psraerobicsbackend.security.handler;

import com.jhanp.psraerobicsbackend.shared.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SecurityJsonResponse implements AuthenticationEntryPoint, AccessDeniedHandler {

    // El ObjectMapper de Spring, no uno nuevo: este ya sabe serializar LocalDateTime
    private final ObjectMapper objectMapper;

    /** No hay token, o es inválido/expirado. */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED,
                "No estás autenticado. Envía un token válido en la cabecera Authorization.");
    }

    /** Hay token válido, pero al usuario le falta el rol necesario. */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN,
                "No tienes permisos para acceder a este recurso.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(status.value(), message));
    }
}
