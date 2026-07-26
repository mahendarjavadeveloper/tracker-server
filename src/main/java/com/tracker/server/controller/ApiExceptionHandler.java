package com.tracker.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(error(exception.getStatusCode().value(), exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        FieldError first = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String message = first == null ? "Invalid request" : first.getField() + " " + first.getDefaultMessage();
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST.value(), message));
    }

    private Map<String, Object> error(int status, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now());
        result.put("status", status);
        result.put("message", message == null ? "Request failed" : message);
        return result;
    }
}
