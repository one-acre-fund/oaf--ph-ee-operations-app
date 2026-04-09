package org.apache.fineract.organisation.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPermissionsDtoTest {

    @DisplayName("all-args constructor sets appUser, permissions, and roles")
    @Test
    void test_all_args_constructor() {
        AppUser appUser = new AppUser();
        appUser.setUsername("kelvin.thuku");
        Set<String> permissions = new LinkedHashSet<>(Set.of("READ_TRANSACTION", "WRITE_TRANSACTION"));
        Set<String> roles = new LinkedHashSet<>(Set.of("Operator", "Viewer"));

        UserPermissionsDto dto = new UserPermissionsDto(appUser, permissions, roles);

        assertEquals(appUser, dto.getAppUser());
        assertEquals(permissions, dto.getPermissions());
        assertEquals(roles, dto.getRoles());
    }

    @DisplayName("setters update all fields")
    @Test
    void test_setters() {
        UserPermissionsDto dto = new UserPermissionsDto(null, null, null);
        AppUser appUser = new AppUser();
        appUser.setUsername("new.user");
        Set<String> permissions = new LinkedHashSet<>(Set.of("ADMIN_ACCESS"));
        Set<String> roles = new LinkedHashSet<>(Set.of("SuperAdmin"));

        dto.setAppUser(appUser);
        dto.setPermissions(permissions);
        dto.setRoles(roles);

        assertEquals(appUser, dto.getAppUser());
        assertEquals(permissions, dto.getPermissions());
        assertEquals(roles, dto.getRoles());
    }

    @DisplayName("dto allows null values")
    @Test
    void test_null_values() {
        UserPermissionsDto dto = new UserPermissionsDto(null, null, null);

        assertNull(dto.getAppUser());
        assertNull(dto.getPermissions());
        assertNull(dto.getRoles());
    }

    @DisplayName("equals and hashCode match for equivalent DTOs")
    @Test
    void test_equals_and_hash_code() {
        AppUser appUser = new AppUser();
        appUser.setUsername("same.user");
        Set<String> permissions = new LinkedHashSet<>(Set.of("READ_TRANSACTION"));
        Set<String> roles = new LinkedHashSet<>(Set.of("Viewer"));

        UserPermissionsDto dto1 = new UserPermissionsDto(appUser, permissions, roles);
        UserPermissionsDto dto2 = new UserPermissionsDto(appUser, new LinkedHashSet<>(permissions), new LinkedHashSet<>(roles));

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    // Additional comprehensive @Data coverage tests

    @DisplayName("getters return correct values")
    @Test
    void test_getters() {
        AppUser appUser = new AppUser();
        appUser.setUsername("test.user");
        Set<String> permissions = new LinkedHashSet<>(Set.of("READ_TRANSACTION", "WRITE_TRANSACTION"));
        Set<String> roles = new LinkedHashSet<>(Set.of("Operator"));

        UserPermissionsDto dto = new UserPermissionsDto(appUser, permissions, roles);

        assertEquals(appUser, dto.getAppUser());
        assertEquals(permissions, dto.getPermissions());
        assertEquals(roles, dto.getRoles());
    }

    @DisplayName("toString includes all field information")
    @Test
    void test_toString() {
        AppUser appUser = new AppUser();
        appUser.setUsername("test.user");
        Set<String> permissions = new LinkedHashSet<>(Set.of("READ_TRANSACTION"));
        Set<String> roles = new LinkedHashSet<>(Set.of("Operator"));

        UserPermissionsDto dto = new UserPermissionsDto(appUser, permissions, roles);

        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("UserPermissionsDto"));
        assertTrue(toString.contains("appUser"));
        assertTrue(toString.contains("permissions"));
        assertTrue(toString.contains("roles"));
    }

    @DisplayName("toString handles null fields gracefully")
    @Test
    void test_toString_with_nulls() {
        UserPermissionsDto dto = new UserPermissionsDto(null, null, null);

        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("UserPermissionsDto"));
        assertTrue(toString.contains("null"));
    }

    @DisplayName("equals returns false for null object")
    @Test
    void test_equals_with_null() {
        AppUser appUser = new AppUser();
        UserPermissionsDto dto = new UserPermissionsDto(appUser, Set.of(), Set.of());

        assertNotEquals(dto, null);
    }

    @DisplayName("equals returns false for different class")
    @Test
    void test_equals_with_different_class() {
        AppUser appUser = new AppUser();
        UserPermissionsDto dto = new UserPermissionsDto(appUser, Set.of(), Set.of());

        assertNotEquals(dto, "not a UserPermissionsDto");
    }

    @DisplayName("equals returns true for same object reference")
    @Test
    void test_equals_same_reference() {
        AppUser appUser = new AppUser();
        UserPermissionsDto dto = new UserPermissionsDto(appUser, Set.of(), Set.of());

        assertEquals(dto, dto);
    }

    @DisplayName("equals returns false when appUser differs")
    @Test
    void test_equals_different_app_user() {
        AppUser appUser1 = new AppUser();
        appUser1.setUsername("user1");
        AppUser appUser2 = new AppUser();
        appUser2.setUsername("user2");
        Set<String> permissions = Set.of("READ_TRANSACTION");
        Set<String> roles = Set.of("Operator");

        UserPermissionsDto dto1 = new UserPermissionsDto(appUser1, permissions, roles);
        UserPermissionsDto dto2 = new UserPermissionsDto(appUser2, permissions, roles);

        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("equals returns false when permissions differ")
    @Test
    void test_equals_different_permissions() {
        AppUser appUser = new AppUser();
        appUser.setUsername("user");
        Set<String> permissions1 = Set.of("READ_TRANSACTION");
        Set<String> permissions2 = Set.of("WRITE_TRANSACTION");
        Set<String> roles = Set.of("Operator");

        UserPermissionsDto dto1 = new UserPermissionsDto(appUser, permissions1, roles);
        UserPermissionsDto dto2 = new UserPermissionsDto(appUser, permissions2, roles);

        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("equals returns false when roles differ")
    @Test
    void test_equals_different_roles() {
        AppUser appUser = new AppUser();
        appUser.setUsername("user");
        Set<String> permissions = Set.of("READ_TRANSACTION");
        Set<String> roles1 = Set.of("Operator");
        Set<String> roles2 = Set.of("Viewer");

        UserPermissionsDto dto1 = new UserPermissionsDto(appUser, permissions, roles1);
        UserPermissionsDto dto2 = new UserPermissionsDto(appUser, permissions, roles2);

        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("equals handles null appUser correctly")
    @Test
    void test_equals_with_null_app_user() {
        Set<String> permissions = Set.of("READ_TRANSACTION");
        Set<String> roles = Set.of("Operator");

        UserPermissionsDto dto1 = new UserPermissionsDto(null, permissions, roles);
        UserPermissionsDto dto2 = new UserPermissionsDto(null, permissions, roles);
        AppUser appUser = new AppUser();
        UserPermissionsDto dto3 = new UserPermissionsDto(appUser, permissions, roles);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
    }

    @DisplayName("equals handles null permissions correctly")
    @Test
    void test_equals_with_null_permissions() {
        AppUser appUser = new AppUser();
        appUser.setUsername("user");
        Set<String> roles = Set.of("Operator");

        UserPermissionsDto dto1 = new UserPermissionsDto(appUser, null, roles);
        UserPermissionsDto dto2 = new UserPermissionsDto(appUser, null, roles);
        UserPermissionsDto dto3 = new UserPermissionsDto(appUser, Set.of("READ_TRANSACTION"), roles);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
    }

    @DisplayName("equals handles null roles correctly")
    @Test
    void test_equals_with_null_roles() {
        AppUser appUser = new AppUser();
        appUser.setUsername("user");
        Set<String> permissions = Set.of("READ_TRANSACTION");

        UserPermissionsDto dto1 = new UserPermissionsDto(appUser, permissions, null);
        UserPermissionsDto dto2 = new UserPermissionsDto(appUser, permissions, null);
        UserPermissionsDto dto3 = new UserPermissionsDto(appUser, permissions, Set.of("Operator"));

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
    }

    @DisplayName("equals handles all null fields correctly")
    @Test
    void test_equals_all_null_fields() {
        UserPermissionsDto dto1 = new UserPermissionsDto(null, null, null);
        UserPermissionsDto dto2 = new UserPermissionsDto(null, null, null);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("hashCode is consistent across multiple calls")
    @Test
    void test_hashcode_consistency() {
        AppUser appUser = new AppUser();
        appUser.setUsername("user");
        Set<String> permissions = Set.of("READ_TRANSACTION");
        Set<String> roles = Set.of("Operator");

        UserPermissionsDto dto = new UserPermissionsDto(appUser, permissions, roles);

        int hash1 = dto.hashCode();
        int hash2 = dto.hashCode();
        int hash3 = dto.hashCode();

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @DisplayName("setting fields via setters maintains equals contract")
    @Test
    void test_equals_after_setter_modifications() {
        AppUser appUser1 = new AppUser();
        appUser1.setUsername("user1");
        AppUser appUser2 = new AppUser();
        appUser2.setUsername("user1");  // Same username
        
        UserPermissionsDto dto1 = new UserPermissionsDto(appUser1, Set.of(), Set.of());
        UserPermissionsDto dto2 = new UserPermissionsDto(null, null, null);
        
        // Initially different
        assertNotEquals(dto1, dto2);
        
        // Set dto2 to match dto1
        dto2.setAppUser(appUser2);
        dto2.setPermissions(Set.of());
        dto2.setRoles(Set.of());
        
        // Now they should be equal (assuming AppUser.equals works properly)
        // Note: This test assumes AppUser has proper equals implementation
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("empty collections are handled correctly in equals")
    @Test
    void test_equals_with_empty_collections() {
        AppUser appUser = new AppUser();
        appUser.setUsername("user");

        UserPermissionsDto dto1 = new UserPermissionsDto(appUser, Set.of(), Set.of());
        UserPermissionsDto dto2 = new UserPermissionsDto(appUser, new LinkedHashSet<>(), new LinkedHashSet<>());

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}
