package org.apache.fineract.test;

import org.apache.fineract.data.ApiGlobalErrorResponse;
import org.apache.fineract.exception.NoAuthorizationException;
import org.apache.fineract.exception.mapper.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("Handle AuthenticationException with message")
    @Test
    void test_handle_authentication_exception_with_message() {
        // Arrange
        String errorMessage = "Invalid credentials provided";
        AuthenticationException exception = new BadCredentialsException(errorMessage);

        // Act
        ResponseEntity<ApiGlobalErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertEquals(401, (int) response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    @DisplayName("Handle AuthenticationException with null message")
    @Test
    void test_handle_authentication_exception_with_null_message() {
        // Arrange
        AuthenticationException exception = new BadCredentialsException(null);

        // Act
        ResponseEntity<ApiGlobalErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Authentication failed", response.getBody().getMessage());
        assertEquals(401, (int) response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
    }

    @DisplayName("Handle NoAuthorizationException with message")
    @Test
    void test_handle_no_authorization_exception_with_message() {
        // Arrange
        String errorMessage = "User does not have permission to access this resource";
        NoAuthorizationException exception = new NoAuthorizationException(errorMessage);

        // Act
        ResponseEntity<ApiGlobalErrorResponse> response = globalExceptionHandler.handleNoAuthorizationException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertEquals(403, (int) response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
    }

    @DisplayName("Handle NoAuthorizationException with empty message")
    @Test
    void test_handle_no_authorization_exception_with_empty_message() {
        // Arrange
        String errorMessage = "";
        NoAuthorizationException exception = new NoAuthorizationException(errorMessage);

        // Act
        ResponseEntity<ApiGlobalErrorResponse> response = globalExceptionHandler.handleNoAuthorizationException(exception);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertEquals(403, (int) response.getBody().getStatus());
    }

    @DisplayName("Verify response status codes are correct")
    @Test
    void test_verify_response_status_codes() {
        // Arrange
        AuthenticationException authException = new BadCredentialsException("Auth error");
        NoAuthorizationException noAuthException = new NoAuthorizationException("Authorization error");

        // Act
        ResponseEntity<ApiGlobalErrorResponse> authResponse = globalExceptionHandler.handleAuthenticationException(authException);
        ResponseEntity<ApiGlobalErrorResponse> noAuthResponse = globalExceptionHandler.handleNoAuthorizationException(noAuthException);

        // Assert
        assertEquals(401, authResponse.getStatusCodeValue());
        assertEquals(403, noAuthResponse.getStatusCodeValue());
    }

    @DisplayName("Verify response body contains proper error structure")
    @Test
    void test_verify_response_body_structure() {
        // Arrange
        String authMessage = "Authentication failed";
        AuthenticationException exception = new BadCredentialsException(authMessage);

        // Act
        ResponseEntity<ApiGlobalErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception);

        // Assert
        ApiGlobalErrorResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.getError());
        assertNotNull(body.getMessage());
        assertNotNull(body.getStatus());
    }
}
