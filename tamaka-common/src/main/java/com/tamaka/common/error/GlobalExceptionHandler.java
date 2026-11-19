package com.tamaka.common.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.tamaka.common.storage.ImagenInvalidaException;
import com.tamaka.common.storage.StorageNotAvailableException;

/**
 * Manejo centralizado de errores (guia de controladores §13). No contiene logica de dominio.
 * 500 nunca expone stack trace al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> uploadLimit(MaxUploadSizeExceededException ex) {
        Map<String, String> details = Map.of("imagen", "La imagen supera el maximo de 5 MB");
        return build(HttpStatus.BAD_REQUEST, "La solicitud no es valida", details);
    }

    @ExceptionHandler(ImagenInvalidaException.class)
    public ResponseEntity<ErrorResponse> imagenInvalida(ImagenInvalidaException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getDetalles());
    }

    @ExceptionHandler(StorageNotAvailableException.class)
    public ResponseEntity<ErrorResponse> storageNoDisponible(StorageNotAvailableException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> businessRule(BusinessException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> details.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "La solicitud no es valida", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> malformedJson(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON valido", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> badParameter(MethodArgumentTypeMismatchException ex) {
        Map<String, String> details = Map.of(ex.getName(), "valor invalido");
        return build(HttpStatus.BAD_REQUEST, "Parametro invalido", details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tiene permiso para esta operacion", null);
    }

    /** Violaciones de constraints de la base (UNIQUE, indices parciales): conflicto con el estado actual. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException ex) {
        log.warn("Conflicto de integridad en base de datos");
        return build(HttpStatus.CONFLICT, "La operacion entra en conflicto con el estado actual", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> unauthenticated(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales invalidas o cuenta no disponible", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, Map<String, String> details) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message, details);
        return ResponseEntity.status(status).body(body);
    }
}
