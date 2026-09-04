package com.jhanp.psraerobicsbackend.shared.exception;


import com.jhanp.psraerobicsbackend.shared.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.sql.SQLException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // Error 400 - Datos inválidos desde el Service
    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidData(InvalidDataException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Error 400 - Datos inválidos en el Controller
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(" | "));

        return build(HttpStatus.BAD_REQUEST, errors);
    }

    // Error 400 - Parámetros inválidos en path/query
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, extractMethodValidationErrors(ex));
    }

    // Error 400 - Validaciones de método
    @ExceptionHandler(MethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(MethodValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, extractMethodValidationErrors(ex));
    }

    // Error 400 - Constraint violations
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(" | "));

        return build(HttpStatus.BAD_REQUEST, errors);
    }

    // Error 400 - JSON Incorrecto
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud JSON es inválido o está mal formado");
    }

    // Error 404 - Recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // Error 409 - Duplicado
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Error 409 - La persona existe pero está desactivada (SQLSTATE 75001)
    @ExceptionHandler(DeactivatedPersonException.class)
    public ResponseEntity<ErrorResponse> handleDeactivatedPerson(DeactivatedPersonException ex) {
        return build(HttpStatus.CONFLICT, DeactivatedPersonException.CODE, ex.getMessage());
    }

    // Error 409 - No hay clase vigente en este momento (SQLSTATE 75002)
    @ExceptionHandler(NoActiveSessionException.class)
    public ResponseEntity<ErrorResponse> handleNoActiveSession(NoActiveSessionException ex) {
        return build(HttpStatus.CONFLICT, NoActiveSessionException.CODE, ex.getMessage());
    }

    /**
     * Traduce aquí, y no en cada Service, los dos SQLSTATE propios del sistema. Se capturan por
     * código y nunca por el texto del mensaje.
     * <p>
     * Son dos situaciones distintas para el usuario final — "¿reactivar a esta persona?" contra
     * "no hay clase ahora" — así que van con códigos distintos aunque ambas respondan 409.
     * Cualquier otro fallo de base de datos sigue siendo un 500.
     * <p>
     * Se lee de SQLException y no de PSQLException a propósito: getSQLState() es del estándar JDBC,
     * y el driver de Postgres está en scope runtime — no está en el classpath de compilación.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        if (ex.getRootCause() instanceof SQLException sql) {
            String sqlState = sql.getSQLState();

            if ("75001".equals(sqlState)) {
                return handleDeactivatedPerson(new DeactivatedPersonException(
                        "La persona está desactivada. Debe reactivarse antes de continuar"));
            }
            if ("75002".equals(sqlState)) {
                return handleNoActiveSession(new NoActiveSessionException(
                        "No hay ninguna clase vigente en este momento"));
            }
        }

        log.error("Error de base de datos no controlado: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    // Error 500 - Problema con el servidor
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Error no controlado: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return build(status, null, message);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), code, message));
    }

    private String extractMethodValidationErrors(MethodValidationResult validationResult) {
        String errors = validationResult.getParameterValidationResults()
                .stream()
                .flatMap(result -> {
                    String parameterName = result.getMethodParameter().getParameterName();

                    if (result instanceof ParameterErrors parameterErrors) {
                        return parameterErrors.getFieldErrors()
                                .stream()
                                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage());
                    }

                    return result.getResolvableErrors()
                            .stream()
                            .map(error -> formatParameterError(parameterName, error));
                })
                .collect(Collectors.joining(" | "));

        if (!errors.isBlank()) {
            return errors;
        }

        return validationResult.getCrossParameterValidationResults()
                .stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(" | "));
    }

    private String formatParameterError(String parameterName, MessageSourceResolvable error) {
        String safeParameterName = parameterName != null ? parameterName : "parametro";
        return safeParameterName + ": " + error.getDefaultMessage();
    }
}

