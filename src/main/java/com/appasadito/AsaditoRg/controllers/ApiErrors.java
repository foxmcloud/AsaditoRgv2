package com.appasadito.AsaditoRg.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.exception.ConstraintViolationException;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message", "Revisá los datos ingresados", "fields", fields));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> malformed() {
        return ResponseEntity.badRequest().body(Map.of("message", "Formato inválido: revisá números, fechas y campos obligatorios"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("message", Objects.requireNonNullElse(ex.getReason(), "No se pudo completar la operación")));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        String sqlState = "unknown";
        String constraint = "";
        boolean reusedEntityId = false;
        String causeType = ex.getClass().getSimpleName();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = ex; cause != null && seen.add(cause); cause = cause.getCause()) {
            causeType = cause.getClass().getSimpleName();
            if ((cause instanceof org.hibernate.NonUniqueObjectException || cause instanceof javax.persistence.EntityExistsException)) reusedEntityId = true;
            if (cause instanceof ConstraintViolationException) {
                constraint = Objects.toString(((ConstraintViolationException) cause).getConstraintName(), "");
            }
            if (cause instanceof SQLException) {
                SQLException sql = (SQLException) cause;
                Set<SQLException> sqlSeen = Collections.newSetFromMap(new IdentityHashMap<>());
                while (sql != null && sqlSeen.add(sql)) {
                    if (sql.getSQLState() != null && sql.getSQLState().matches("[0-9A-Z]{5}")) {
                        sqlState = sql.getSQLState();
                        break;
                    }
                    sql = sql.getNextException();
                }
            }
        }
        String code = "DB_INTEGRITY";
        String message = "La base de datos rechazó el guardado. Revisá el diagnóstico del servidor con esta referencia.";
        switch (sqlState) {
            case "23505":
                if (constraint.toLowerCase(Locale.ROOT).endsWith("_pkey")) {
                    code = "DB_ID_CONFLICT";
                    message = "El identificador generado ya está ocupado. Hay que revisar la secuencia de IDs de la base de datos.";
                } else {
                    code = "DB_DUPLICATE";
                    message = "El guardado entra en conflicto con una restricción de unicidad de la base de datos.";
                }
                break;
            case "23502":
                code = "DB_REQUIRED_VALUE";
                message = "Falta un valor obligatorio para la base de datos. Revisá que el esquema coincida con esta versión.";
                break;
            case "23503":
                code = "DB_REFERENCE_CONFLICT";
                message = "La base de datos rechazó una relación del registro. Actualizá el catálogo y verificá el corte y el local.";
                break;
            case "23514":
                code = "DB_CHECK_CONSTRAINT";
                message = "Los datos no cumplen una regla definida en la base de datos.";
                break;
            case "22003":
                code = "DB_NUMERIC_RANGE";
                message = "Un número supera la capacidad de la columna en la base de datos. Revisá el importe y el esquema.";
                break;
            default: break;
        }
        // Hibernate can detect a reused generated ID before it sends any SQL.
        if (reusedEntityId) {
            code = "DB_ID_CONFLICT";
            message = "El identificador generado ya está ocupado. Hay que revisar la secuencia de IDs de la base de datos.";
        }
        String reference = UUID.randomUUID().toString();
        // Do not log the exception text, SQL or submitted values (registration also uses this advice).
        LOG.warn("Guardado rechazado: reference={} code={} sqlState={} cause={}", reference, code, sqlState, causeType);
        return ResponseEntity.status(409).body(Map.of("message", message, "code", code, "reference", reference));
    }
}
