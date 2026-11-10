package com.appasadito.AsaditoRg.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> validacion(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatus()).body(Map.of("message", error.getReason()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> baseDeDatos() {
        return ResponseEntity.status(409).body(Map.of("message", "La base rechazó el guardado. Revisá el diagnóstico de precios y la secuencia de IDs si importaste datos."));
    }
}
