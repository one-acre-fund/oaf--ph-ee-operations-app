package org.apache.fineract.organisation.role;

import org.apache.fineract.organisation.permission.PermissionData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionsDataTest {

    @DisplayName("Constructor with all arguments creates object correctly")
    @Test
    void test_all_args_constructor() {
        // Arrange
        Long id = 1L;
        String name = "Admin";
        String description = "Administrator role";
        Boolean disabled = false;
        PermissionData permission1 = new PermissionData("authorization", "READ_USER", "USER", "READ", true);
        PermissionData permission2 = new PermissionData("authorization", "CREATE_USER", "USER", "CREATE", false);
        Collection<PermissionData> permissions = Arrays.asList(permission1, permission2);

        // Act
        RolePermissionsData rolePermissionsData = new RolePermissionsData(id, name, description, disabled, permissions);

        // Assert
        assertEquals(id, rolePermissionsData.getId());
        assertEquals(name, rolePermissionsData.getName());
        assertEquals(description, rolePermissionsData.getDescription());
        assertEquals(disabled, rolePermissionsData.getDisabled());
        assertEquals(permissions, rolePermissionsData.getPermissionUsageData());
        assertEquals(2, rolePermissionsData.getPermissionUsageData().size());
    }

    @DisplayName("Default constructor creates object with null fields")
    @Test
    void test_default_constructor() {
        // Act
        RolePermissionsData rolePermissionsData = new RolePermissionsData();

        // Assert
        assertNull(rolePermissionsData.getId());
        assertNull(rolePermissionsData.getName());
        assertNull(rolePermissionsData.getDescription());
        assertNull(rolePermissionsData.getDisabled());
        assertNull(rolePermissionsData.getPermissionUsageData());
    }

    @DisplayName("Setters update fields correctly")
    @Test
    void test_setters() {
        // Arrange
        RolePermissionsData rolePermissionsData = new RolePermissionsData();
        Long id = 2L;
        String name = "User";
        String description = "Standard user role";
        Boolean disabled = true;
        Collection<PermissionData> permissions = new ArrayList<>();

        // Act
        rolePermissionsData.setId(id);
        rolePermissionsData.setName(name);
        rolePermissionsData.setDescription(description);
        rolePermissionsData.setDisabled(disabled);
        rolePermissionsData.setPermissionUsageData(permissions);

        // Assert
        assertEquals(id, rolePermissionsData.getId());
        assertEquals(name, rolePermissionsData.getName());
        assertEquals(description, rolePermissionsData.getDescription());
        assertEquals(disabled, rolePermissionsData.getDisabled());
        assertEquals(permissions, rolePermissionsData.getPermissionUsageData());
    }

    @DisplayName("Getters return correct values")
    @Test
    void test_getters() {
        // Arrange
        Long id = 3L;
        String name = "Manager";
        String description = "Manager role";
        Boolean disabled = false;
        PermissionData permission = new PermissionData("authorization", "UPDATE_USER", "USER", "UPDATE", true);
        Collection<PermissionData> permissions = Arrays.asList(permission);

        RolePermissionsData rolePermissionsData = new RolePermissionsData(id, name, description, disabled, permissions);

        // Act & Assert
        assertEquals(id, rolePermissionsData.getId());
        assertEquals(name, rolePermissionsData.getName());
        assertEquals(description, rolePermissionsData.getDescription());
        assertEquals(disabled, rolePermissionsData.getDisabled());
        assertEquals(permissions, rolePermissionsData.getPermissionUsageData());
    }

    @DisplayName("Equals returns true for identical objects")
    @Test
    void test_equals_identical_objects() {
        // Arrange
        Collection<PermissionData> permissions = Arrays.asList(
                new PermissionData("authorization", "READ_USER", "USER", "READ", true)
        );
        RolePermissionsData data1 = new RolePermissionsData(1L, "Admin", "Admin role", false, permissions);
        RolePermissionsData data2 = new RolePermissionsData(1L, "Admin", "Admin role", false, permissions);

        // Act & Assert
        assertEquals(data1, data2);
        assertEquals(data1.hashCode(), data2.hashCode());
    }

    @DisplayName("Equals returns false for different objects")
    @Test
    void test_equals_different_objects() {
        // Arrange
        Collection<PermissionData> permissions1 = Arrays.asList(
                new PermissionData("authorization", "READ_USER", "USER", "READ", true)
        );
        Collection<PermissionData> permissions2 = Arrays.asList(
                new PermissionData("authorization", "CREATE_USER", "USER", "CREATE", false)
        );
        
        RolePermissionsData data1 = new RolePermissionsData(1L, "Admin", "Admin role", false, permissions1);
        RolePermissionsData data2 = new RolePermissionsData(2L, "User", "User role", true, permissions2);

        // Act & Assert
        assertNotEquals(data1, data2);
    }

    @DisplayName("Equals returns true for same instance")
    @Test
    void test_equals_same_instance() {
        // Arrange
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", false, new ArrayList<>());

        // Act & Assert
        assertEquals(data, data);
    }

    @DisplayName("Equals returns false when compared with null")
    @Test
    void test_equals_null() {
        // Arrange
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", false, new ArrayList<>());

        // Act & Assert
        assertNotEquals(data, null);
    }

    @DisplayName("Equals returns false when compared with different class")
    @Test
    void test_equals_different_class() {
        // Arrange
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", false, new ArrayList<>());
        String differentObject = "Not a RolePermissionsData";

        // Act & Assert
        assertNotEquals(data, differentObject);
    }

    @DisplayName("HashCode is consistent for equal objects")
    @Test
    void test_hashCode_consistency() {
        // Arrange
        Collection<PermissionData> permissions = Arrays.asList(
                new PermissionData("authorization", "READ_USER", "USER", "READ", true)
        );
        RolePermissionsData data1 = new RolePermissionsData(1L, "Admin", "Admin role", false, permissions);
        RolePermissionsData data2 = new RolePermissionsData(1L, "Admin", "Admin role", false, permissions);

        // Act & Assert
        assertEquals(data1.hashCode(), data2.hashCode());
    }

    @DisplayName("ToString returns non-null string")
    @Test
    void test_toString() {
        // Arrange
        Collection<PermissionData> permissions = Arrays.asList(
                new PermissionData("authorization", "READ_USER", "USER", "READ", true)
        );
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", false, permissions);

        // Act
        String result = data.toString();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Admin"));
        assertTrue(result.contains("1"));
    }

    @DisplayName("Constructor handles null permissions collection")
    @Test
    void test_null_permissions() {
        // Act
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", false, null);

        // Assert
        assertEquals(1L, (long) data.getId());
        assertEquals("Admin", data.getName());
        assertEquals("Admin role", data.getDescription());
        assertEquals(false, data.getDisabled());
        assertNull(data.getPermissionUsageData());
    }

    @DisplayName("Constructor handles empty permissions collection")
    @Test
    void test_empty_permissions() {
        // Arrange
        Collection<PermissionData> emptyPermissions = new ArrayList<>();

        // Act
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", false, emptyPermissions);

        // Assert
        assertEquals(1L, (long) data.getId());
        assertNotNull(data.getPermissionUsageData());
        assertEquals(0, data.getPermissionUsageData().size());
    }

    @DisplayName("Constructor handles null name and description")
    @Test
    void test_null_name_and_description() {
        // Act
        RolePermissionsData data = new RolePermissionsData(1L, null, null, false, new ArrayList<>());

        // Assert
        assertEquals(1, (long) data.getId());
        assertNull(data.getName());
        assertNull(data.getDescription());
        assertEquals(false, data.getDisabled());
    }

    @DisplayName("Constructor handles null disabled flag")
    @Test
    void test_null_disabled_flag() {
        // Act
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", null, new ArrayList<>());

        // Assert
        assertEquals(1, (long) data.getId());
        assertNull(data.getDisabled());
    }

    @DisplayName("Constructor handles large permissions collection")
    @Test
    void test_large_permissions_collection() {
        // Arrange
        List<PermissionData> permissions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            permissions.add(new PermissionData("group" + i, "CODE_" + i, "ENTITY_" + i, "ACTION_" + i, i % 2 == 0));
        }

        // Act
        RolePermissionsData data = new RolePermissionsData(1L, "Admin", "Admin role", false, permissions);

        // Assert
        assertEquals(100, data.getPermissionUsageData().size());
    }

    @DisplayName("Setter allows modifying permissions collection")
    @Test
    void test_modify_permissions_via_setter() {
        // Arrange
        RolePermissionsData data = new RolePermissionsData();
        Collection<PermissionData> initialPermissions = Arrays.asList(
                new PermissionData("auth", "READ", "USER", "READ", true)
        );
        Collection<PermissionData> updatedPermissions = Arrays.asList(
                new PermissionData("auth", "READ", "USER", "READ", true),
                new PermissionData("auth", "CREATE", "USER", "CREATE", false)
        );

        // Act
        data.setPermissionUsageData(initialPermissions);
        assertEquals(1, data.getPermissionUsageData().size());

        data.setPermissionUsageData(updatedPermissions);

        // Assert
        assertEquals(2, data.getPermissionUsageData().size());
    }

    @DisplayName("Object with all null fields equals another with all null fields")
    @Test
    void test_equals_all_nulls() {
        // Arrange
        RolePermissionsData data1 = new RolePermissionsData();
        RolePermissionsData data2 = new RolePermissionsData();

        // Act & Assert
        assertEquals(data1, data2);
        assertEquals(data1.hashCode(), data2.hashCode());
    }
}
