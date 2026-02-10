package org.apache.fineract.test;

import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.role.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role();
    }

    @DisplayName("hasPermissionTo - returns true when role has the permission")
    @Test
    void test_has_permission_to_returns_true() {
        // Arrange
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(Collections.singletonList(permission));

        // Act
        boolean result = role.hasPermissionTo("READ_TRANSFER");

        // Assert
        assertTrue(result);
    }

    @DisplayName("hasPermissionTo - returns false when role does not have the permission")
    @Test
    void test_has_permission_to_returns_false() {
        // Arrange
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(Collections.singletonList(permission));

        // Act
        boolean result = role.hasPermissionTo("CREATE_USER");

        // Assert
        assertFalse(result);
    }

    @DisplayName("hasPermissionTo - returns false when permissions collection is empty")
    @Test
    void test_has_permission_to_with_empty_permissions() {
        // Arrange
        role.setPermissions(Collections.emptyList());

        // Act
        boolean result = role.hasPermissionTo("READ_TRANSFER");

        // Assert
        assertFalse(result);
    }

    @DisplayName("hasPermissionTo - returns true when permission exists among multiple permissions")
    @Test
    void test_has_permission_to_with_multiple_permissions() {
        // Arrange
        Permission permission1 = new Permission();
        permission1.setCode("READ_TRANSFER");

        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        Permission permission3 = new Permission();
        permission3.setCode("UPDATE_AUDIT");

        role.setPermissions(Arrays.asList(permission1, permission2, permission3));

        // Act
        boolean result = role.hasPermissionTo("CREATE_USER");

        // Assert
        assertTrue(result);
    }

    @DisplayName("hasPermissionTo - returns false when searching for non-existent permission in multiple")
    @Test
    void test_has_permission_to_not_found_in_multiple() {
        // Arrange
        Permission permission1 = new Permission();
        permission1.setCode("READ_TRANSFER");

        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        role.setPermissions(Arrays.asList(permission1, permission2));

        // Act
        boolean result = role.hasPermissionTo("DELETE_TRANSFER");

        // Assert
        assertFalse(result);
    }

    @DisplayName("hasPermissionTo - returns true for first matching permission")
    @Test
    void test_has_permission_to_stops_at_first_match() {
        // Arrange
        Permission permission1 = new Permission();
        permission1.setCode("ALL_FUNCTIONS");

        Permission permission2 = new Permission();
        permission2.setCode("READ_TRANSFER");

        role.setPermissions(Arrays.asList(permission1, permission2));

        // Act
        boolean result = role.hasPermissionTo("ALL_FUNCTIONS");

        // Assert
        assertTrue(result);
    }

    @DisplayName("hasPermissionTo - case insensitive permission check")
    @Test
    void test_has_permission_to_case_sensitive() {
        // Arrange
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(Collections.singletonList(permission));

        boolean result = role.hasPermissionTo("read_transfer");

        assertTrue(result);
    }

    @DisplayName("hasPermissionTo - returns false when searching with null permission code")
    @Test
    void test_has_permission_to_with_null_code() {
        // Arrange
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(Collections.singletonList(permission));

        // Act
        boolean result = role.hasPermissionTo(null);

        // Assert
        assertFalse(result);
    }

    @DisplayName("hasPermissionTo - returns false when searching with empty string")
    @Test
    void test_has_permission_to_with_empty_string() {
        // Arrange
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(Collections.singletonList(permission));

        // Act
        boolean result = role.hasPermissionTo("");

        // Assert
        assertFalse(result);
    }

    @DisplayName("hasPermissionTo - handles special permission codes")
    @Test
    void test_has_permission_to_with_special_codes() {
        // Arrange
        Permission allFunctions = new Permission();
        allFunctions.setCode("ALL_FUNCTIONS");

        Permission allFunctionsRead = new Permission();
        allFunctionsRead.setCode("ALL_FUNCTIONS_READ");

        role.setPermissions(Arrays.asList(allFunctions, allFunctionsRead));

        // Act & Assert
        assertTrue(role.hasPermissionTo("ALL_FUNCTIONS"));
        assertTrue(role.hasPermissionTo("ALL_FUNCTIONS_READ"));
    }

    @DisplayName("hasPermissionTo - works with HashSet collection")
    @Test
    void test_has_permission_to_with_hashset() {
        // Arrange
        Permission permission1 = new Permission();
        permission1.setCode("READ_TRANSFER");

        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        role.setPermissions(new HashSet<>(Arrays.asList(permission1, permission2)));

        // Act
        boolean result1 = role.hasPermissionTo("READ_TRANSFER");
        boolean result2 = role.hasPermissionTo("CREATE_USER");
        boolean result3 = role.hasPermissionTo("DELETE_TRANSFER");

        // Assert
        assertTrue(result1);
        assertTrue(result2);
        assertFalse(result3);
    }

    @DisplayName("hasPermissionTo - exact match required")
    @Test
    void test_has_permission_to_exact_match_required() {
        // Arrange
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(Collections.singletonList(permission));

        // Act
        boolean result1 = role.hasPermissionTo("READ");
        boolean result2 = role.hasPermissionTo("TRANSFER");
        boolean result3 = role.hasPermissionTo("READ_TRANSFER_EXTRA");

        // Assert
        assertFalse(result1);
        assertFalse(result2);
        assertFalse(result3);
    }
}