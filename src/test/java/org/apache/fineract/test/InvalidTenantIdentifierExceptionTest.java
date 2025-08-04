package org.apache.fineract.test;

import org.apache.fineract.config.security.exception.InvalidTenantIdentifierException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InvalidTenantIdentifierExceptionTest {

    @Test
    void testExceptionMessage() {
        String expectedMessage = "Invalid tenant identifier";
        InvalidTenantIdentifierException exception = new InvalidTenantIdentifierException(expectedMessage);
        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testExceptionWithCause() {
        String expectedMessage = "Invalid tenant identifier";
        EmptyResultDataAccessException cause = new EmptyResultDataAccessException(1);
        InvalidTenantIdentifierException exception = new InvalidTenantIdentifierException(expectedMessage, cause);
        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
