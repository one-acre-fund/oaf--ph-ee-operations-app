package org.apache.fineract.test;

import org.apache.fineract.exception.NoAuthorizationException;
import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserPermissionTest {

    private AppUser appUser;

    @Mock
    private Role mockRole;

    @Mock
    private Permission mockPermission;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        appUser = new AppUser();
    }

    // validateHasReadPermission tests
    @DisplayName("Validate read permission - user has READ permission")
    @Test
    void test_validate_has_read_permission_success() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasReadPermission("TRANSFER"));
    }

    @DisplayName("Validate read permission - user lacks READ permission")
    @Test
    void test_validate_has_read_permission_fails() {
        // Arrange
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        NoAuthorizationException exception = assertThrows(
                NoAuthorizationException.class,
                () -> appUser.validateHasReadPermission("TRANSFER")
        );
        assertEquals("User has no authority to READ transfers", exception.getMessage());
    }

    // validateHasCreatePermission tests
    @DisplayName("Validate create permission - user has CREATE permission")
    @Test
    void test_validate_has_create_permission_success() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("CREATE_USER");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasCreatePermission("USER"));
    }

    @DisplayName("Validate create permission - user lacks CREATE permission")
    @Test
    void test_validate_has_create_permission_fails() {
        // Arrange
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        NoAuthorizationException exception = assertThrows(
                NoAuthorizationException.class,
                () -> appUser.validateHasCreatePermission("USER")
        );
        assertEquals("User has no authority to CREATE users", exception.getMessage());
    }

    // validateHasUpdatePermission tests
    @DisplayName("Validate update permission - user has UPDATE permission")
    @Test
    void test_validate_has_update_permission_success() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("UPDATE_AUDIT");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasUpdatePermission("AUDIT"));
    }

    @DisplayName("Validate update permission - user lacks UPDATE permission")
    @Test
    void test_validate_has_update_permission_fails() {
        // Arrange
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        NoAuthorizationException exception = assertThrows(
                NoAuthorizationException.class,
                () -> appUser.validateHasUpdatePermission("AUDIT")
        );
        assertEquals("User has no authority to UPDATE audits", exception.getMessage());
    }

    // validateHasDeletePermission tests
    @DisplayName("Validate delete permission - user has DELETE permission")
    @Test
    void test_validate_has_delete_permission_success() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("DELETE_TRANSFER");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasDeletePermission("TRANSFER"));
    }

    @DisplayName("Validate delete permission - user lacks DELETE permission")
    @Test
    void test_validate_has_delete_permission_fails() {
        // Arrange
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        NoAuthorizationException exception = assertThrows(
                NoAuthorizationException.class,
                () -> appUser.validateHasDeletePermission("TRANSFER")
        );
        assertEquals("User has no authority to DELETE transfers", exception.getMessage());
    }

    // validateHasExportPermission tests
    @DisplayName("Validate export permission - user has EXPORT permission")
    @Test
    void test_validate_has_export_permission_success() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("EXPORT_TRANSFER");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasExportPermission("TRANSFER"));
    }

    @DisplayName("Validate export permission - user lacks EXPORT permission")
    @Test
    void test_validate_has_export_permission_fails() {
        // Arrange
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        NoAuthorizationException exception = assertThrows(
                NoAuthorizationException.class,
                () -> appUser.validateHasExportPermission("TRANSFER")
        );
        assertEquals("User has no authority to EXPORT transfers", exception.getMessage());
    }

    // validateHasPermission tests
    @DisplayName("Validate permission - user has ALL_FUNCTIONS permission")
    @Test
    void test_validate_has_permission_with_all_functions() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("ALL_FUNCTIONS");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasPermission("READ", "TRANSFER"));
    }

    @DisplayName("Validate permission - user has ALL_FUNCTIONS_READ permission")
    @Test
    void test_validate_has_permission_with_all_functions_read() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("ALL_FUNCTIONS_READ");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasPermission("READ", "TRANSFER"));
    }

    @DisplayName("Validate permission - user has specific permission")
    @Test
    void test_validate_has_permission_with_specific_permission() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("CUSTOM_ACTION");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasPermission("CUSTOM", "ACTION"));
    }

    @DisplayName("Validate permission - user lacks permission")
    @Test
    void test_validate_has_permission_fails() {
        // Arrange
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        appUser.setRoles(Collections.singletonList(role));

        // Act & Assert
        NoAuthorizationException exception = assertThrows(
                NoAuthorizationException.class,
                () -> appUser.validateHasPermission("APPROVE", "TRANSFER")
        );
        assertEquals("User has no authority to APPROVE transfers", exception.getMessage());
    }

    // hasNotPermissionForAnyOf tests
    @DisplayName("hasNotPermissionForAnyOf - user has none of the permissions")
    @Test
    void test_has_not_permission_for_any_of_returns_true() {
        // Arrange
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        appUser.setRoles(Collections.singletonList(role));

        // Act
        boolean result = appUser.hasNotPermissionForAnyOf("READ_TRANSFER", "CREATE_TRANSFER");

        // Assert
        assertTrue(result);
    }

    @DisplayName("hasNotPermissionForAnyOf - user has at least one permission")
    @Test
    void test_has_not_permission_for_any_of_returns_false() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act
        boolean result = appUser.hasNotPermissionForAnyOf("READ_TRANSFER", "CREATE_TRANSFER");

        // Assert
        assertFalse(result);
    }

    @DisplayName("hasNotPermissionForAnyOf - user has ALL_FUNCTIONS")
    @Test
    void test_has_not_permission_for_any_of_with_all_functions() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("ALL_FUNCTIONS");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act
        boolean result = appUser.hasNotPermissionForAnyOf("READ_TRANSFER", "CREATE_TRANSFER");

        // Assert
        assertFalse(result);
    }

    // hasAllFunctionsPermission tests
    @DisplayName("hasAllFunctionsPermission - user has ALL_FUNCTIONS permission")
    @Test
    void test_has_all_functions_permission_returns_true() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("ALL_FUNCTIONS");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act
        boolean result = appUser.hasNotPermissionForAnyOf("SOME_PERMISSION");

        // Assert - user has all functions, so should not lack permission
        assertFalse(result);
    }

    @DisplayName("hasAllFunctionsPermission - user lacks ALL_FUNCTIONS permission")
    @Test
    void test_has_all_functions_permission_returns_false() {
        // Arrange
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("READ_TRANSFER");
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        // Act
        boolean result = appUser.hasNotPermissionForAnyOf("CREATE_USER");

        // Assert
        assertTrue(result);
    }

    @DisplayName("Multiple roles - user has permission from second role")
    @Test
    void test_multiple_roles_permission_check() {
        // Arrange
        Role role1 = new Role();
        role1.setPermissions(new HashSet<>());

        Role role2 = new Role();
        Permission permission = new Permission();
        permission.setCode("READ_AUDIT");
        role2.setPermissions(new HashSet<>(Collections.singletonList(permission)));

        appUser.setRoles(Arrays.asList(role1, role2));

        // Act & Assert
        assertDoesNotThrow(() -> appUser.validateHasReadPermission("AUDIT"));
    }

    @DisplayName("Empty roles - all permission checks should fail")
    @Test
    void test_empty_roles_all_permissions_fail() {
        // Arrange
        appUser.setRoles(Collections.emptyList());

        // Act & Assert
        assertThrows(NoAuthorizationException.class, () -> appUser.validateHasReadPermission("TRANSFER"));
        assertThrows(NoAuthorizationException.class, () -> appUser.validateHasCreatePermission("USER"));
        assertThrows(NoAuthorizationException.class, () -> appUser.validateHasUpdatePermission("AUDIT"));
        assertThrows(NoAuthorizationException.class, () -> appUser.validateHasDeletePermission("TRANSFER"));
        assertThrows(NoAuthorizationException.class, () -> appUser.validateHasExportPermission("TRANSFER"));
    }

    @DisplayName("Case sensitivity - permission codes should be case insensitive")
    @Test
    void test_permission_case_sensitivity() {
        Role role = new Role();
        Permission permission = new Permission();
        permission.setCode("read_transfer"); // lowercase
        role.setPermissions(new HashSet<>(Collections.singletonList(permission)));
        appUser.setRoles(Collections.singletonList(role));

        assertDoesNotThrow(() -> appUser.validateHasReadPermission("TRANSFER"));
    }
}
