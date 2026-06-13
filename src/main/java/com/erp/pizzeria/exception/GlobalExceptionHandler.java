package com.erp.pizzeria.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Order(1)
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> handleStock(StockInsuficienteException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), ex.getFaltantes());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(this::campoError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Datos invalidos", errores);
    }

    private Map<String, String> campoError(FieldError fe) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("campo", fe.getField());
        error.put("mensaje", fe.getDefaultMessage());
        return error;
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensaje, Object detalle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        if (detalle != null) {
            body.put("detalle", detalle);
        }
        return ResponseEntity.status(status).body(body);
    }
}
