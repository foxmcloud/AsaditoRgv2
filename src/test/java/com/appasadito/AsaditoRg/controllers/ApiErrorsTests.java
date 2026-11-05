package com.appasadito.AsaditoRg.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import java.sql.SQLException;
import java.sql.BatchUpdateException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApiErrorsTests {
    private final ApiErrors advice = new ApiErrors();
    @ParameterizedTest
    @CsvSource({"23505,DB_DUPLICATE", "23502,DB_REQUIRED_VALUE", "23503,DB_REFERENCE_CONFLICT", "23514,DB_CHECK_CONSTRAINT", "22003,DB_NUMERIC_RANGE", "99999,DB_INTEGRITY"})
    void classifiesWithoutExposingDatabaseValues(String state, String code) {
        var response = advice.conflict(new DataIntegrityViolationException("sensitive-wrapper", new SQLException("sensitive-value", state)));
        assertEquals(409, response.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(code, body.get("code"));
        assertNotNull(body.get("reference"));
        assertFalse(body.toString().contains("sensitive"));
        assertFalse(body.containsKey("sql"));
    }
    @Test void recognizesGeneratedPrimaryKeyCollision() {
        var cause = new ConstraintViolationException("private database error", new SQLException("secret", "23505"), "precios_pkey");
        Map<?, ?> body = (Map<?, ?>) advice.conflict(new DataIntegrityViolationException("wrapper", cause)).getBody();
        assertEquals("DB_ID_CONFLICT", body.get("code"));
        assertFalse(body.toString().contains("precios_pkey"));
        assertFalse(body.toString().contains("secret"));
    }
    @Test void readsBatchedSqlErrorsAndHandlesMissingDetails() {
        BatchUpdateException batch = new BatchUpdateException();
        batch.setNextException(new SQLException("private", "23502"));
        Map<?, ?> body = (Map<?, ?>) advice.conflict(new DataIntegrityViolationException("wrapper", batch)).getBody();
        assertEquals("DB_REQUIRED_VALUE", body.get("code"));
        Map<?, ?> fallback = (Map<?, ?>) advice.conflict(new DataIntegrityViolationException("private")).getBody();
        assertEquals("DB_INTEGRITY", fallback.get("code"));
    }
    @Test void detectsReusedEntityIdBeforeAnySqlIsSent() {
        for (RuntimeException cause : java.util.List.of(
                new org.hibernate.NonUniqueObjectException("private", 123, "private-entity"),
                new javax.persistence.EntityExistsException("private"))) {
            Map<?, ?> body = (Map<?, ?>) advice.conflict(new DataIntegrityViolationException("wrapper", cause)).getBody();
            assertEquals("DB_ID_CONFLICT", body.get("code"));
            assertFalse(body.toString().contains("private"));
            assertFalse(body.containsKey("id"));
        }
    }
}
