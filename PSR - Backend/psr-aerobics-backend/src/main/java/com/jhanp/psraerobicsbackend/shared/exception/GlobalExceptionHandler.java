package com.jhanp.psraerobicsbackend.shared.exception;


import com.jhanp.psraerobicsbackend.shared.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    // Error 400 - Falta un @RequestParam obligatorio
    // Sin este handler la excepción caía en handleGeneral y el front recibía un 500 por algo que es
    // culpa de la petición: omitir desde/hasta en auditoría respondía "error interno del servidor".
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName());
    }

    // Error 400 - El parámetro llegó, pero no se puede convertir al tipo esperado
    // Los dos juntos porque para el cliente son el mismo problema. Se distingue el mismatch solo para
    // poder decirle cuál de los parámetros falla, que es lo único accionable.
    // MethodArgumentNotValidException también hereda de BindException, pero tiene su propio handler
    // arriba y Spring elige siempre el más específico, así que no se lo come este.
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleBinding(Exception ex) {
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return build(HttpStatus.BAD_REQUEST, "El parámetro '" + mismatch.getName() + "' tiene un valor inválido");
        }
        return build(HttpStatus.BAD_REQUEST, "Los parámetros de la solicitud son inválidos");
    }

    /**
     * Error 401 - Credenciales incorrectas, cuenta desactivada o identificador inexistente.
     * <p>
     * Sin este handler, el BadCredentialsException que lanza AuthenticationManager.authenticate()
     * dentro de AuthService.login caía en handleGeneral: una contraseña mal escrita respondía 500 y
     * el frontend no tenía forma de distinguirla de un fallo del servidor. El
     * AuthenticationEntryPoint de SecurityConfig no la cubre — solo atiende lo que pasa en la cadena
     * de filtros, y esto ocurre ya dentro del controller.
     * <p>
     * Un solo mensaje para todos los casos, a propósito: distinguir "no existe ese usuario" de
     * "contraseña incorrecta" le confirma a quien prueba credenciales qué documentos y correos están
     * registrados. DisabledException (cuenta desactivada) entra por aquí y responde igual.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Autenticación fallida: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    /**
     * Error 401 - La cuenta existe y la contraseña es correcta, pero está desactivada.
     * <p>
     * Va ANTES del handler de AuthenticationException a propósito — bueno, en realidad da igual el
     * orden en el archivo: Spring elige siempre el @ExceptionHandler más específico, y
     * DisabledException es subclase de AuthenticationException. Se deja aquí junto a su hermano para
     * que se lean seguidos.
     * <p>
     * <b>Esto es un trade-off consciente, no una mejora gratuita.</b> Distinguir "cuenta bloqueada"
     * de "credenciales incorrectas" convierte el login en un oráculo: quien pruebe documentos o
     * correos puede averiguar cuáles existen y están desactivados (enumeración de cuentas). Se acepta
     * porque para una academia de aeróbicos el riesgo real es bajo y el usuario legítimo necesita
     * saber por qué no entra. Si se quisiera revertir, basta con borrar este método.
     * <p>
     * El frontend debe mostrar "cuenta bloqueada" SOLO cuando venga este code; cualquier otro 401,
     * incluido uno sin code, es el mensaje genérico.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        log.warn("Intento de acceso con cuenta desactivada");
        return build(HttpStatus.UNAUTHORIZED, "CUENTA_DESACTIVADA",
                "Tu cuenta está desactivada. Comunícate con recepción.");
    }

    /**
     * Error 403 - Autenticado, pero sin el rol necesario.
     * <p>
     * Cubre también AuthorizationDeniedException, que es la que lanza @PreAuthorize y hereda de
     * AccessDeniedException. Se lanza dentro de la invocación del controller, así que la resuelve
     * este advice y no el accessDeniedHandler de SecurityConfig (ese solo ve los rechazos de la
     * cadena de filtros); sin este handler, un permiso insuficiente se veía como 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso.");
    }

    /**
     * Error 404 - La RUTA no existe (distinto de "el recurso no existe", que es el handler de abajo).
     * <p>
     * Sin esto, cualquier URL mal escrita de toda la API respondía 500: el
     * {@code @ExceptionHandler(Exception.class)} del final se tragaba también estas excepciones y las
     * convertía en "error interno del servidor". Un frontend que llamaba a un endpoint inexistente
     * —por ejemplo {@code PATCH /api/metodos-pago/3/desactivar} antes de que existiera— recibía un
     * 500 y perdía el tiempo buscando el fallo en el servidor en vez de en la URL.
     * <p>
     * Se cubren las dos porque Spring lanza una u otra según cómo esté configurado el despacho:
     * NoHandlerFoundException cuando no hay handler, y NoResourceFoundException cuando la petición
     * cae en el manejador de recursos estáticos.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNoHandler(Exception ex) {
        log.warn("Ruta no encontrada: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "La ruta solicitada no existe");
    }

    // Error 404 - Recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Error 409 - Choque con una regla de negocio que no es un duplicado.
     * <p>
     * ConflictException existía desde el principio pero nadie la lanzaba y nadie la atendía: caía en
     * handleGeneral y habría salido como 500 el día que alguien la usara. Hoy la usa el tope de cinco
     * contactos de emergencia.
     * <p>
     * Se distingue de DuplicateResourceException porque no todo 409 es "ya existe otro igual": aquí
     * el recurso es válido, lo que no lo admite es el estado actual.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
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
        // Los CHECK del esquema son reglas de negocio escritas en SQL: que se rompan es culpa de la
        // petición, no del servidor, así que son 400 y no 500. Se identifican por el NOMBRE del
        // constraint, nunca por el texto del mensaje, que cambia entre versiones de Postgres.
        String constraint = violatedConstraint(ex);

        if ("chk_sesion_horas".equals(constraint)) {
            return build(HttpStatus.BAD_REQUEST,
                    "La hora de fin debe ser posterior a la hora de inicio");
        }
        if ("chk_contacto_emergencia_no_vacio".equals(constraint)) {
            return build(HttpStatus.BAD_REQUEST,
                    "El contacto de emergencia necesita al menos un nombre o un teléfono");
        }

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

    /**
     * El nombre del CHECK o del UNIQUE que se violó, o null si el fallo no es una violación de
     * restricción.
     * <p>
     * Se lee de {@link ConstraintViolationException} de Hibernate y no del texto del
     * {@link SQLException}: el mensaje de Postgres está traducido según el locale del servidor y
     * cambia entre versiones, así que buscar subcadenas ahí es frágil. El nombre del constraint es
     * estable — lo fijamos nosotros en el DDL.
     */
    private String violatedConstraint(DataAccessException ex) {
        Throwable cause = ex.getCause();

        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException hibernateEx) {
                return hibernateEx.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
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

