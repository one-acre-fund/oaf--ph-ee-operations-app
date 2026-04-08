package org.apache.fineract.organisation.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserPermissionsDtoTest {

    private static final Long ID = 1L;
    private static final String USERNAME = "kelvin.thuku";
    private static final String EMAIL = "kelvin.thuku@oneacrefund.org";
    private static final Set<String> PERMISSIONS = new LinkedHashSet<>(Arrays.asList("READ_TRANSACTION", "WRITE_TRANSACTION"));
    private static final Set<String> ROLES = new LinkedHashSet<>(Arrays.asList("Operator", "Viewer"));

    // ---- Constructor & getters ----

    @Test
    @DisplayName("All-args constructor sets all fields correctly")
    void test_all_args_constructor() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);

        assertEquals(ID, dto.getId());
        assertEquals(USERNAME, dto.getUsername());
        assertEquals(EMAIL, dto.getEmail());
        assertEquals(PERMISSIONS, dto.getPermissions());
        assertEquals(ROLES, dto.getRoles());
    }

    @Test
    @DisplayName("Constructor with null permissions and roles stores nulls")
    void test_constructor_null_collections() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, null, null);

        assertNull(dto.getPermissions());
        assertNull(dto.getRoles());
    }

    @Test
    @DisplayName("Constructor with null id, username, and email stores nulls")
    void test_constructor_null_scalar_fields() {
        UserPermissionsDto dto = new UserPermissionsDto(null, null, null, PERMISSIONS, ROLES);

        assertNull(dto.getId());
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
    }

    @Test
    @DisplayName("Constructor with empty permissions and roles sets empty sets")
    void test_constructor_empty_collections() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, Collections.emptySet(), Collections.emptySet());

        assertNotNull(dto.getPermissions());
        assertTrue(dto.getPermissions().isEmpty());
        assertNotNull(dto.getRoles());
        assertTrue(dto.getRoles().isEmpty());
    }

    // ---- Setters ----

    @Test
    @DisplayName("Setters update all fields correctly")
    void test_setters() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);

        Set<String> newPermissions = new HashSet<>(Collections.singletonList("ADMIN_ACCESS"));
        Set<String> newRoles = new HashSet<>(Collections.singletonList("SuperAdmin"));

        dto.setId(99L);
        dto.setUsername("new.user");
        dto.setEmail("new.user@oneacrefund.org");
        dto.setPermissions(newPermissions);
        dto.setRoles(newRoles);

        assertEquals(Long.valueOf(99), dto.getId());
        assertEquals("new.user", dto.getUsername());
        assertEquals("new.user@oneacrefund.org", dto.getEmail());
        assertEquals(newPermissions, dto.getPermissions());
        assertEquals(newRoles, dto.getRoles());
    }

    @Test
    @DisplayName("Setter can clear permissions to null")
    void test_setter_clears_permissions_to_null() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        dto.setPermissions(null);
        assertNull(dto.getPermissions());
    }

    @Test
    @DisplayName("Setter can clear roles to null")
    void test_setter_clears_roles_to_null() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        dto.setRoles(null);
        assertNull(dto.getRoles());
    }

    // ---- equals ----

    @Test
    @DisplayName("equals returns true for two objects with the same field values")
    void test_equals_same_values() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));

        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns true for same instance")
    void test_equals_same_instance() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertEquals(dto, dto);
    }

    @Test
    @DisplayName("equals returns false for different id")
    void test_equals_different_id() {
        UserPermissionsDto dto1 = new UserPermissionsDto(1L, USERNAME, EMAIL, PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(2L, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false for different username")
    void test_equals_different_username() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, "alice", EMAIL, PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, "bob", EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false for different email")
    void test_equals_different_email() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, "a@example.com", PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, "b@example.com", PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false for different permissions")
    void test_equals_different_permissions() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(Collections.singletonList("READ_TRANSACTION")), ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(Collections.singletonList("WRITE_TRANSACTION")), ROLES);
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false for different roles")
    void test_equals_different_roles() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, new HashSet<>(Collections.singletonList("Operator")));
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, new HashSet<>(Collections.singletonList("Viewer")));
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when compared to null")
    void test_equals_null() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(null, dto);
    }

    @Test
    @DisplayName("equals returns false when compared to a different type")
    void test_equals_different_type() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals("not a dto", dto);
    }

    @Test
    @DisplayName("equals returns true when both objects have null id")
    void test_equals_both_null_id() {
        UserPermissionsDto dto1 = new UserPermissionsDto(null, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        UserPermissionsDto dto2 = new UserPermissionsDto(null, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when one id is null and the other is not")
    void test_equals_one_null_id() {
        UserPermissionsDto dto1 = new UserPermissionsDto(null, USERNAME, EMAIL, PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto2, dto1);
    }

    @Test
    @DisplayName("equals returns true when both objects have null username")
    void test_equals_both_null_username() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, null, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, null, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when one username is null and the other is not")
    void test_equals_one_null_username() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, null, EMAIL, PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto2, dto1);
    }

    @Test
    @DisplayName("equals returns true when both objects have null email")
    void test_equals_both_null_email() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, null, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, null, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when one email is null and the other is not")
    void test_equals_one_null_email() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, null, PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto2, dto1);
    }

    @Test
    @DisplayName("equals returns true when both objects have null permissions")
    void test_equals_both_null_permissions() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, null, new HashSet<>(ROLES));
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, null, new HashSet<>(ROLES));
        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when one permissions set is null and the other is not")
    void test_equals_one_null_permissions() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, null, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto2, dto1);
    }

    @Test
    @DisplayName("equals returns true when both objects have null roles")
    void test_equals_both_null_roles() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), null);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), null);
        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when one roles set is null and the other is not")
    void test_equals_one_null_roles() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, null);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertNotEquals(dto1, dto2);
        assertNotEquals(dto2, dto1);
    }

    @Test
    @DisplayName("equals returns true when all fields are null in both objects")
    void test_equals_all_null_fields() {
        UserPermissionsDto dto1 = new UserPermissionsDto(null, null, null, null, null);
        UserPermissionsDto dto2 = new UserPermissionsDto(null, null, null, null, null);
        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("canEqual returns true for another UserPermissionsDto instance")
    void test_canEqual_same_type() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertTrue(dto1.canEqual(dto2));
    }

    @Test
    @DisplayName("canEqual returns false for a non-UserPermissionsDto object")
    void test_canEqual_different_type() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertFalse(dto.canEqual("some string"));
        assertFalse(dto.canEqual(42));
        assertFalse(dto.canEqual(null));
    }

    // ---- hashCode ----

    @Test
    @DisplayName("hashCode is equal for two objects with the same field values")
    void test_hashCode_same_values() {
        UserPermissionsDto dto1 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));
        UserPermissionsDto dto2 = new UserPermissionsDto(ID, USERNAME, EMAIL, new HashSet<>(PERMISSIONS), new HashSet<>(ROLES));

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("hashCode differs for objects with different field values")
    void test_hashCode_different_values() {
        UserPermissionsDto dto1 = new UserPermissionsDto(1L, "alice", "a@example.com", PERMISSIONS, ROLES);
        UserPermissionsDto dto2 = new UserPermissionsDto(2L, "bob", "b@example.com", PERMISSIONS, ROLES);

        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("hashCode is consistent across multiple calls on the same object")
    void test_hashCode_consistency() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        int first = dto.hashCode();
        int second = dto.hashCode();
        assertEquals(first, second);
    }

    @Test
    @DisplayName("hashCode does not throw when id is null")
    void test_hashCode_null_id() {
        UserPermissionsDto dto = new UserPermissionsDto(null, USERNAME, EMAIL, PERMISSIONS, ROLES);
        assertDoesNotThrow(dto::hashCode);
    }

    @Test
    @DisplayName("hashCode does not throw when username is null")
    void test_hashCode_null_username() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, null, EMAIL, PERMISSIONS, ROLES);
        assertDoesNotThrow(dto::hashCode);
    }

    @Test
    @DisplayName("hashCode does not throw when email is null")
    void test_hashCode_null_email() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, null, PERMISSIONS, ROLES);
        assertDoesNotThrow(dto::hashCode);
    }

    @Test
    @DisplayName("hashCode does not throw when permissions is null")
    void test_hashCode_null_permissions() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, null, ROLES);
        assertDoesNotThrow(dto::hashCode);
    }

    @Test
    @DisplayName("hashCode does not throw when roles is null")
    void test_hashCode_null_roles() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, null);
        assertDoesNotThrow(dto::hashCode);
    }

    @Test
    @DisplayName("hashCode is consistent for two objects with all null fields")
    void test_hashCode_all_null_fields() {
        UserPermissionsDto dto1 = new UserPermissionsDto(null, null, null, null, null);
        UserPermissionsDto dto2 = new UserPermissionsDto(null, null, null, null, null);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    // ---- toString ----

    @Test
    @DisplayName("toString contains all field values")
    void test_toString_contains_all_fields() {
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, ROLES);
        String result = dto.toString();

        assertTrue(result.contains(ID.toString()));
        assertTrue(result.contains(USERNAME));
        assertTrue(result.contains(EMAIL));
        assertTrue(result.contains("READ_TRANSACTION"));
        assertTrue(result.contains("WRITE_TRANSACTION"));
        assertTrue(result.contains("Operator"));
        assertTrue(result.contains("Viewer"));
    }

    @Test
    @DisplayName("toString handles null fields without throwing")
    void test_toString_with_nulls() {
        UserPermissionsDto dto = new UserPermissionsDto(null, null, null, null, null);
        assertDoesNotThrow(dto::toString);
        String result = dto.toString();
        assertNotNull(result);
    }

    // ---- Collection integrity ----

    @Test
    @DisplayName("Permissions set returned is the same reference stored")
    void test_permissions_set_reference() {
        Set<String> permissions = new LinkedHashSet<>(Arrays.asList("READ_TRANSACTION"));
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, permissions, ROLES);
        assertSame(permissions, dto.getPermissions());
    }

    @Test
    @DisplayName("Roles set returned is the same reference stored")
    void test_roles_set_reference() {
        Set<String> roles = new LinkedHashSet<>(Arrays.asList("Operator"));
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, PERMISSIONS, roles);
        assertSame(roles, dto.getRoles());
    }

    @Test
    @DisplayName("Permissions set with a single entry is stored correctly")
    void test_single_permission() {
        Set<String> single = new HashSet<>(Collections.singletonList("READ_TRANSACTION"));
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, single, Collections.emptySet());

        assertEquals(1, dto.getPermissions().size());
        assertTrue(dto.getPermissions().contains("READ_TRANSACTION"));
    }

    @Test
    @DisplayName("Roles set with a single entry is stored correctly")
    void test_single_role() {
        Set<String> single = new HashSet<>(Collections.singletonList("Operator"));
        UserPermissionsDto dto = new UserPermissionsDto(ID, USERNAME, EMAIL, Collections.emptySet(), single);

        assertEquals(1, dto.getRoles().size());
        assertTrue(dto.getRoles().contains("Operator"));
    }
}
