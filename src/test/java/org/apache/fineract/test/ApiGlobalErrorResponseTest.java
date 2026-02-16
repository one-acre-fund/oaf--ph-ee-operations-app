package org.apache.fineract.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.data.ApiGlobalErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // ========== Constructors ==========

    @DisplayName("Test no-args constructor")
    @Test
    void test_no_args_constructor() {
        // Act
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();

        // Assert
        assertNotNull(response);
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNull(response.getStatus());
    }

    @DisplayName("Test all-args constructor")
    @Test
    void test_all_args_constructor() {
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

    @DisplayName("Test all-args constructor with null values")
    @Test
    void test_all_args_constructor_with_nulls() {
        // Act
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse(null, null, null);

        // Assert
        assertNotNull(response);
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNull(response.getStatus());
    }

    // ========== Getters ==========

    @DisplayName("Test getError returns correct value")
    @Test
    void test_get_error() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("TestError", "Message", 400);

        // Act
        String error = response.getError();

        // Assert
        assertEquals("TestError", error);
    }

    @DisplayName("Test getMessage returns correct value")
    @Test
    void test_get_message() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "TestMessage", 400);

        // Act
        String message = response.getMessage();

        // Assert
        assertEquals("TestMessage", message);
    }

    @DisplayName("Test getStatus returns correct value")
    @Test
    void test_get_status() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "Message", 500);

        // Act
        Integer status = response.getStatus();

        // Assert
        assertEquals(500, (int) status);
    }

    // ========== Setters ==========

    @DisplayName("Test setError sets correct value")
    @Test
    void test_set_error() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();

        // Act
        response.setError("NewError");

        // Assert
        assertEquals("NewError", response.getError());
    }

    @DisplayName("Test setMessage sets correct value")
    @Test
    void test_set_message() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();

        // Act
        response.setMessage("NewMessage");

        // Assert
        assertEquals("NewMessage", response.getMessage());
    }

    @DisplayName("Test setStatus sets correct value")
    @Test
    void test_set_status() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();

        // Act
        response.setStatus(404);

        // Assert
        assertEquals(404, (int) response.getStatus());
    }

    @DisplayName("Test all setters")
    @Test
    void test_all_setters() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse();

        // Act
        response.setError("CustomError");
        response.setMessage("CustomMessage");
        response.setStatus(418);

        // Assert
        assertEquals("CustomError", response.getError());
        assertEquals("CustomMessage", response.getMessage());
        assertEquals(418, (int) response.getStatus());
    }

    @DisplayName("Test setter with null values")
    @Test
    void test_setters_with_null_values() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act
        response.setError(null);
        response.setMessage(null);
        response.setStatus(null);

        // Assert
        assertNull(response.getError());
        assertNull(response.getMessage());
        assertNull(response.getStatus());
    }

    @DisplayName("Test modifying object after creation")
    @Test
    void test_modify_after_creation() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Original", "Original", 100);

        // Act
        response.setError("Modified");
        response.setMessage("Modified");
        response.setStatus(200);

        // Assert
        assertEquals("Modified", response.getError());
        assertEquals("Modified", response.getMessage());
        assertEquals(200, (int) response.getStatus());
    }

    // ========== Equals ==========

    @DisplayName("Test equals with same object")
    @Test
    void test_equals_same_object() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act & Assert
        assertEquals(response, response);
        assertTrue(response.equals(response));
    }

    @DisplayName("Test equals with equal objects")
    @Test
    void test_equals_equal_objects() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error", "Message", 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act & Assert
        assertEquals(response1, response2);
        assertEquals(response2, response1);
    }

    @DisplayName("Test equals with different error")
    @Test
    void test_equals_different_error() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error1", "Message", 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error2", "Message", 400);

        // Act & Assert
        assertNotEquals(response1, response2);
    }

    @DisplayName("Test equals with different message")
    @Test
    void test_equals_different_message() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error", "Message1", 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error", "Message2", 400);

        // Act & Assert
        assertNotEquals(response1, response2);
    }

    @DisplayName("Test equals with different status")
    @Test
    void test_equals_different_status() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error", "Message", 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error", "Message", 500);

        // Act & Assert
        assertNotEquals(response1, response2);
    }

    @DisplayName("Test equals with null")
    @Test
    void test_equals_with_null() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act & Assert
        assertNotEquals(response, null);
        assertFalse(response.equals(null));
    }

    @DisplayName("Test equals with different class")
    @Test
    void test_equals_with_different_class() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "Message", 400);
        String differentObject = "Not an ApiGlobalErrorResponse";

        // Act & Assert
        assertNotEquals(response, differentObject);
        assertFalse(response.equals(differentObject));
    }

    @DisplayName("Test equals with null fields")
    @Test
    void test_equals_with_null_fields() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse(null, null, null);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse(null, null, null);

        // Act & Assert
        assertEquals(response1, response2);
    }

    @DisplayName("Test equals with one null field")
    @Test
    void test_equals_with_one_null_field() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error", null, 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error", null, 400);

        // Act & Assert
        assertEquals(response1, response2);
    }

    @DisplayName("Test equals when one has null and other has value")
    @Test
    void test_equals_null_vs_value() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error", null, 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act & Assert
        assertNotEquals(response1, response2);
    }

    // ========== HashCode ==========

    @DisplayName("Test hashCode for equal objects")
    @Test
    void test_hashcode_equal_objects() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error", "Message", 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act & Assert
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @DisplayName("Test hashCode for different objects")
    @Test
    void test_hashcode_different_objects() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse("Error1", "Message", 400);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse("Error2", "Message", 400);

        // Act & Assert
        assertNotEquals(response1.hashCode(), response2.hashCode());
    }

    @DisplayName("Test hashCode consistency")
    @Test
    void test_hashcode_consistency() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act
        int hashCode1 = response.hashCode();
        int hashCode2 = response.hashCode();

        // Assert
        assertEquals(hashCode1, hashCode2);
    }

    @DisplayName("Test hashCode with null fields")
    @Test
    void test_hashcode_with_null_fields() {
        // Arrange
        ApiGlobalErrorResponse response1 = new ApiGlobalErrorResponse(null, null, null);
        ApiGlobalErrorResponse response2 = new ApiGlobalErrorResponse(null, null, null);

        // Act & Assert
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    // ========== ToString ==========

    @DisplayName("Test toString contains all fields")
    @Test
    void test_toString_contains_all_fields() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("TestError", "TestMessage", 418);

        // Act
        String toString = response.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("TestError"));
        assertTrue(toString.contains("TestMessage"));
        assertTrue(toString.contains("418"));
    }

    @DisplayName("Test toString with null fields")
    @Test
    void test_toString_with_null_fields() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse(null, null, null);

        // Act
        String toString = response.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("null"));
    }

    @DisplayName("Test toString format")
    @Test
    void test_toString_format() {
        // Arrange
        ApiGlobalErrorResponse response = new ApiGlobalErrorResponse("Error", "Message", 400);

        // Act
        String toString = response.toString();

        // Assert
        assertTrue(toString.contains("ApiGlobalErrorResponse"));
        assertTrue(toString.contains("error="));
        assertTrue(toString.contains("message="));
        assertTrue(toString.contains("status="));
    }

    // ========== JSON Serialization ==========

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
}