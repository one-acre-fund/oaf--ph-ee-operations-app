package org.apache.fineract.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.data.ApiGlobalErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiGlobalErrorResponseTest {

    @DisplayName("Create unauthorized error response with message")
    @Test
    void test_create_unauthorized_error_response() {
        // Arrange
        String message = "Invalid credentials";

        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.unauthorized(message);

        // Assert
        assertNotNull(response);
        assertEquals("Unauthorized", response.getError());
        assertEquals(message, response.getMessage());
        assertEquals(401, (int) response.getStatus());
    }

    @DisplayName("Create forbidden error response with message")
    @Test
    void test_create_forbidden_error_response() {
        // Arrange
        String message = "Access denied";

        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.forbidden(message);

        // Assert
        assertNotNull(response);
        assertEquals("Forbidden", response.getError());
        assertEquals(message, response.getMessage());
        assertEquals(403, (int) response.getStatus());
    }

    @DisplayName("Create unauthorized error response with empty message")
    @Test
    void test_create_unauthorized_with_empty_message() {
        // Arrange
        String message = "";

        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.unauthorized(message);

        // Assert
        assertNotNull(response);
        assertEquals("Unauthorized", response.getError());
        assertEquals("", response.getMessage());
        assertEquals(401, (int) response.getStatus());
    }

    @DisplayName("Create forbidden error response with null message")
    @Test
    void test_create_forbidden_with_null_message() {
        // Arrange
        String message = null;

        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.forbidden(message);

        // Assert
        assertNotNull(response);
        assertEquals("Forbidden", response.getError());
        assertNull(response.getMessage());
        assertEquals(403, (int) response.getStatus());
    }

    @DisplayName("Create error response using no-args constructor")
    @Test
    void test_create_with_no_args_constructor() {
        // Act
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();

        // Assert
        assertNotNull(response);
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNull(response.getStatus());
    }

    @DisplayName("Create error response using all-args constructor")
    @Test
    void test_create_with_all_args_constructor() {
        // Arrange
        String error = "Bad Request";
        String message = "Invalid input";
        Integer status = 400;

        // Act
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse(error, message, status);

        // Assert
        assertNotNull(response);
        assertEquals(error, response.getError());
        assertEquals(message, response.getMessage());
        assertEquals(status, response.getStatus());
    }

    @DisplayName("Test setters and getters")
    @Test
    void test_setters_and_getters() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();
        String error = "Internal Server Error";
        String message = "Something went wrong";
        Integer status = 500;

        // Act
        response.setError(error);
        response.setMessage(message);
        response.setStatus(status);

        // Assert
        assertEquals(error, response.getError());
        assertEquals(message, response.getMessage());
        assertEquals(status, response.getStatus());
    }

    @DisplayName("Verify unauthorized method returns correct status code")
    @Test
    void test_unauthorized_returns_401() {
        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.unauthorized("Test message");

        // Assert
        assertEquals(401, (int) response.getStatus());
    }

    @DisplayName("Verify forbidden method returns correct status code")
    @Test
    void test_forbidden_returns_403() {
        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.forbidden("Test message");

        // Assert
        assertEquals(403, (int) response.getStatus());
    }

    @DisplayName("Verify JSON serialization excludes null fields")
    @Test
    void test_json_serialization_excludes_nulls() throws Exception {
        // Arrange
        ObjectMapper objectMapper = new ObjectMapper();
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();
        response.setError("Error");
        response.setStatus(400);
        // message is null

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertFalse(json.contains("message"));
        assertTrue(json.contains("error"));
        assertTrue(json.contains("status"));
    }

    @DisplayName("Verify JSON serialization includes non-null fields")
    @Test
    void test_json_serialization_includes_non_nulls() throws Exception {
        // Arrange
        ObjectMapper objectMapper = new ObjectMapper();
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.unauthorized("Authentication failed");

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        assertTrue(json.contains("error"));
        assertTrue(json.contains("message"));
        assertTrue(json.contains("status"));
        assertTrue(json.contains("Unauthorized"));
        assertTrue(json.contains("Authentication failed"));
        assertTrue(json.contains("401"));
    }

    @DisplayName("Verify JSON deserialization")
    @Test
    void test_json_deserialization() throws Exception {
        // Arrange
        ObjectMapper objectMapper = new ObjectMapper();
        String json = "{\"error\":\"Unauthorized\",\"message\":\"Invalid token\",\"status\":401}";

        // Act
        ApiGlobalErrorResponse response = objectMapper.readValue(json, ApiGlobalErrorResponse.class);

        // Assert
        assertNotNull(response);
        assertEquals("Unauthorized", response.getError());
        assertEquals("Invalid token", response.getMessage());
        assertEquals(401, (int) response.getStatus());
    }

    @DisplayName("Test equality of two unauthorized responses")
    @Test
    void test_equality_of_responses() {
        // Arrange
        String message = "Same message";
        ApiGlobalErrorResponse response1 = ApiGlobalErrorResponse.unauthorized(message);
        ApiGlobalErrorResponse response2 = ApiGlobalErrorResponse.unauthorized(message);

        // Assert
        assertEquals(response1.getError(), response2.getError());
        assertEquals(response1.getMessage(), response2.getMessage());
        assertEquals(response1.getStatus(), response2.getStatus());
    }

    @DisplayName("Test difference between unauthorized and forbidden")
    @Test
    void test_difference_between_unauthorized_and_forbidden() {
        // Arrange
        String message = "Test message";
        ApiGlobalErrorResponse unauthorized = ApiGlobalErrorResponse.unauthorized(message);
        ApiGlobalErrorResponse forbidden = ApiGlobalErrorResponse.forbidden(message);

        // Assert
        assertNotEquals(unauthorized.getError(), forbidden.getError());
        assertEquals(unauthorized.getMessage(), forbidden.getMessage());
        assertNotEquals(unauthorized.getStatus(), forbidden.getStatus());
        assertEquals("Unauthorized", unauthorized.getError());
        assertEquals("Forbidden", forbidden.getError());
        assertEquals(401, (int) unauthorized.getStatus());
        assertEquals(403, (int) forbidden.getStatus());
    }

    @DisplayName("Test unauthorized with special characters in message")
    @Test
    void test_unauthorized_with_special_characters() {
        // Arrange
        String message = "User has no authority to READ <resource>";

        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.unauthorized(message);

        // Assert
        assertEquals(message, response.getMessage());
        assertEquals("Unauthorized", response.getError());
        assertEquals(401, (int) response.getStatus());
    }

    @DisplayName("Test forbidden with long message")
    @Test
    void test_forbidden_with_long_message() {
        // Arrange
        String message = "User does not have sufficient permissions to perform this action. " +
                "Please contact your administrator for access.";

        // Act
        ApiGlobalErrorResponse response = ApiGlobalErrorResponse.forbidden(message);

        // Assert
        assertEquals(message, response.getMessage());
        assertEquals("Forbidden", response.getError());
        assertEquals(403, (int) response.getStatus());
    }
}
