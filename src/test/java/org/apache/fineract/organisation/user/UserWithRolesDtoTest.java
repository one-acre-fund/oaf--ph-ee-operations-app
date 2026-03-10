package org.apache.fineract.organisation.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserWithRolesDtoTest {

    @Test
    @DisplayName("All-args constructor sets all fields correctly")
    void test_all_args_constructor() {
        List<String> roles = Arrays.asList("Admin", "User");
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", roles);

        assertEquals(1L, (long) dto.getId());
        assertEquals("alice", dto.getUsername());
        assertEquals(roles, dto.getRoles());
    }

    @Test
    @DisplayName("Setters update fields correctly")
    void test_setters() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Collections.emptyList());

        dto.setId(42L);
        dto.setUsername("bob");
        List<String> newRoles = Arrays.asList("Manager");
        dto.setRoles(newRoles);

        assertEquals(42L, (long) dto.getId());
        assertEquals("bob", dto.getUsername());
        assertEquals(newRoles, dto.getRoles());
    }

    @Test
    @DisplayName("equals returns true for two objects with same field values")
    void test_equals_same_values() {
        List<String> roles = Arrays.asList("Admin");
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", roles);
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", roles);

        assertEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false for objects with different field values")
    void test_equals_different_values() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", Arrays.asList("Admin"));
        UserWithRolesDto dto2 = new UserWithRolesDto(2L, "bob", Arrays.asList("User"));

        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when compared to null")
    void test_equals_null() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        assertNotEquals(null, dto);
    }

    @Test
    @DisplayName("equals returns false when compared to a different type")
    void test_equals_different_type() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        assertNotEquals("not a dto", dto);
    }

    @Test
    @DisplayName("equals returns true when same instance")
    void test_equals_same_instance() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Arrays.asList("Admin"));
        assertEquals(dto, dto);
    }

    @Test
    @DisplayName("hashCode is equal for objects with same field values")
    void test_hashCode_same_values() {
        List<String> roles = Arrays.asList("Admin");
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", roles);
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", roles);

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("hashCode differs for objects with different field values")
    void test_hashCode_different_values() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", Arrays.asList("Admin"));
        UserWithRolesDto dto2 = new UserWithRolesDto(2L, "bob", Arrays.asList("User"));

        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("toString contains all field values")
    void test_toString() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Arrays.asList("Admin", "User"));
        String result = dto.toString();

        assertTrue(result.contains("1"));
        assertTrue(result.contains("alice"));
        assertTrue(result.contains("Admin"));
        assertTrue(result.contains("User"));
    }

    @Test
    @DisplayName("Works correctly with null roles list")
    void test_null_roles() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", null);

        assertNull(dto.getRoles());
    }

    @Test
    @DisplayName("Works correctly with empty roles list")
    void test_empty_roles() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Collections.emptyList());

        assertNotNull(dto.getRoles());
        assertTrue(dto.getRoles().isEmpty());
    }

    @Test
    @DisplayName("Works correctly with null id and username")
    void test_null_id_and_username() {
        UserWithRolesDto dto = new UserWithRolesDto(null, null, Collections.emptyList());

        assertNull(dto.getId());
        assertNull(dto.getUsername());
    }

    // --- equals() null-branch conditions ---

    @Test
    @DisplayName("equals returns false when this.id is non-null but other.id is null")
    void test_equals_this_id_non_null_other_id_null() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        UserWithRolesDto dto2 = new UserWithRolesDto(null, "alice", Collections.emptyList());
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when this.id is null but other.id is non-null")
    void test_equals_this_id_null_other_id_non_null() {
        UserWithRolesDto dto1 = new UserWithRolesDto(null, "alice", Collections.emptyList());
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when ids match but this.username is non-null and other.username is null")
    void test_equals_this_username_non_null_other_username_null() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, null, Collections.emptyList());
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when ids match but this.username is null and other.username is non-null")
    void test_equals_this_username_null_other_username_non_null() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, null, Collections.emptyList());
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when id+username match but this.roles is non-null and other.roles is null")
    void test_equals_this_roles_non_null_other_roles_null() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", Arrays.asList("Admin"));
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", null);
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns false when id+username match but this.roles is null and other.roles is non-null")
    void test_equals_this_roles_null_other_roles_non_null() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", null);
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", Arrays.asList("Admin"));
        assertNotEquals(dto1, dto2);
    }

    @Test
    @DisplayName("equals returns true for two objects where all fields are null")
    void test_equals_all_fields_null() {
        UserWithRolesDto dto1 = new UserWithRolesDto(null, null, null);
        UserWithRolesDto dto2 = new UserWithRolesDto(null, null, null);
        assertEquals(dto1, dto2);
    }

    // --- hashCode() null-field conditions ---

    @Test
    @DisplayName("hashCode is consistent when id is null")
    void test_hashCode_null_id() {
        UserWithRolesDto dto1 = new UserWithRolesDto(null, "alice", Arrays.asList("Admin"));
        UserWithRolesDto dto2 = new UserWithRolesDto(null, "alice", Arrays.asList("Admin"));
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("hashCode is consistent when username is null")
    void test_hashCode_null_username() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, null, Arrays.asList("Admin"));
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, null, Arrays.asList("Admin"));
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("hashCode is consistent when roles is null")
    void test_hashCode_null_roles() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", null);
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", null);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("hashCode is consistent when all fields are null")
    void test_hashCode_all_null_fields() {
        UserWithRolesDto dto1 = new UserWithRolesDto(null, null, null);
        UserWithRolesDto dto2 = new UserWithRolesDto(null, null, null);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    @DisplayName("canEqual returns true for same type")
    void test_can_equal_same_type() {
        UserWithRolesDto dto1 = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        UserWithRolesDto dto2 = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        assertTrue(dto1.canEqual(dto2));
    }

    @Test
    @DisplayName("canEqual returns false for different type")
    void test_can_equal_different_type() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        assertFalse(dto.canEqual("string"));
    }

    @Test
    @DisplayName("canEqual returns false for null")
    void test_can_equal_null() {
        UserWithRolesDto dto = new UserWithRolesDto(1L, "alice", Collections.emptyList());
        assertFalse(dto.canEqual(null));
    }
}
