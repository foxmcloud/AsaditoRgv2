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
        return ResponseEntity.status(409).body(Map.of("message", "No se pudo guardar por un conflicto con datos existentes. Actualizá la página antes de volver a intentarlo."));
    }
}
