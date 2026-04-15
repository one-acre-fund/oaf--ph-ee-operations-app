package org.apache.fineract.organisation.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppUserDtoTest {

    @DisplayName("constructor with AppUser sets all fields correctly")
    @Test
    void test_constructor_with_app_user() {
        AppUser appUser = new AppUser();
        appUser.setEmail("test@example.com");
        appUser.setUsername("testuser");
        appUser.setFirstname("Test");
        appUser.setLastname("User");
        appUser.setAccountNonExpired(true);
        appUser.setAccountNonLocked(true);
        appUser.setCredentialsNonExpired(true);
        appUser.setEnabled(true);
        appUser.setFirstTimeLoginRemaining(false);
        appUser.setDeleted(false);
        appUser.setPasswordNeverExpires(false);
        Date lastPasswordUpdate = new Date();
        appUser.setLastTimePasswordUpdated(lastPasswordUpdate);

        Set<String> permissions = new LinkedHashSet<>(Set.of("READ_TRANSACTION", "WRITE_TRANSACTION"));
        Set<String> roles = new LinkedHashSet<>(Set.of("Operator", "Viewer"));

        AppUserDto dto = new AppUserDto(appUser, permissions, roles);

        assertEquals("test@example.com", dto.getEmail());
        assertEquals("testuser", dto.getUsername());
        assertEquals("Test", dto.getFirstname());
        assertEquals("User", dto.getLastname());
        assertEquals(true, dto.isAccountNonExpired());
        assertEquals(true, dto.isAccountNonLocked());
        assertEquals(true, dto.isCredentialsNonExpired());
        assertEquals(true, dto.isEnabled());
        assertEquals(false, dto.isFirstTimeLoginRemaining());
        assertEquals(false, dto.isDeleted());
        assertEquals(false, dto.isPasswordNeverExpires());
        assertEquals(lastPasswordUpdate, dto.getLastTimePasswordUpdated());
        assertEquals(permissions, dto.getPermissions());
        assertEquals(roles, dto.getRoles());
    }

    @DisplayName("no-args constructor creates empty dto")
    @Test
    void test_no_args_constructor() {
        AppUserDto dto = new AppUserDto();

        assertNull(dto.getEmail());
        assertNull(dto.getUsername());
        assertNull(dto.getFirstname());
        assertNull(dto.getLastname());
        assertEquals(false, dto.isAccountNonExpired());
        assertEquals(false, dto.isAccountNonLocked());
        assertEquals(false, dto.isCredentialsNonExpired());
        assertEquals(false, dto.isEnabled());
        assertEquals(false, dto.isFirstTimeLoginRemaining());
        assertEquals(false, dto.isDeleted());
        assertEquals(false, dto.isPasswordNeverExpires());
        assertNull(dto.getLastTimePasswordUpdated());
        assertNull(dto.getPermissions());
        assertNull(dto.getRoles());
    }

    @DisplayName("setters update all fields")
    @Test
    void test_setters() {
        AppUserDto dto = new AppUserDto();
        Set<String> permissions = new LinkedHashSet<>(Set.of("ADMIN_ACCESS"));
        Set<String> roles = new LinkedHashSet<>(Set.of("SuperAdmin"));
        Date date = new Date();

        dto.setEmail("new@example.com");
        dto.setUsername("newuser");
        dto.setFirstname("New");
        dto.setLastname("User");
        dto.setAccountNonExpired(true);
        dto.setAccountNonLocked(true);
        dto.setCredentialsNonExpired(true);
        dto.setEnabled(true);
        dto.setFirstTimeLoginRemaining(true);
        dto.setDeleted(true);
        dto.setPasswordNeverExpires(true);
        dto.setLastTimePasswordUpdated(date);
        dto.setPermissions(permissions);
        dto.setRoles(roles);

        assertEquals("new@example.com", dto.getEmail());
        assertEquals("newuser", dto.getUsername());
        assertEquals("New", dto.getFirstname());
        assertEquals("User", dto.getLastname());
        assertEquals(true, dto.isAccountNonExpired());
        assertEquals(true, dto.isAccountNonLocked());
        assertEquals(true, dto.isCredentialsNonExpired());
        assertEquals(true, dto.isEnabled());
        assertEquals(true, dto.isFirstTimeLoginRemaining());
        assertEquals(true, dto.isDeleted());
        assertEquals(true, dto.isPasswordNeverExpires());
        assertEquals(date, dto.getLastTimePasswordUpdated());
        assertEquals(permissions, dto.getPermissions());
        assertEquals(roles, dto.getRoles());
    }

    @DisplayName("equals and hashCode work correctly")
    @Test
    void test_equals_and_hash_code() {
        Set<String> permissions = new LinkedHashSet<>(Set.of("READ_TRANSACTION"));
        Set<String> roles = new LinkedHashSet<>(Set.of("Viewer"));

        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("same.user");
        dto1.setEmail("same@example.com");
        dto1.setPermissions(permissions);
        dto1.setRoles(roles);

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("same.user");
        dto2.setEmail("same@example.com");
        dto2.setPermissions(new LinkedHashSet<>(permissions));
        dto2.setRoles(new LinkedHashSet<>(roles));

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("toString includes class name")
    @Test
    void test_toString() {
        AppUserDto dto = new AppUserDto();
        dto.setUsername("test.user");
        dto.setPermissions(Set.of("READ_TRANSACTION"));
        dto.setRoles(Set.of("Operator"));

        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("AppUserDto"));
    }

    @DisplayName("toString handles null fields gracefully")
    @Test
    void test_toString_with_nulls() {
        AppUserDto dto = new AppUserDto();

        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("AppUserDto"));
    }

    @DisplayName("equals returns false for null object")
    @Test
    void test_equals_with_null() {
        AppUserDto dto = new AppUserDto();
        dto.setUsername("test");

        assertNotEquals(dto, null);
    }

    @DisplayName("equals returns false for different class")
    @Test
    void test_equals_with_different_class() {
        AppUserDto dto = new AppUserDto();
        dto.setUsername("test");

        assertNotEquals(dto, "not an AppUserDto");
    }

    @DisplayName("equals returns true for same object reference")
    @Test
    void test_equals_same_reference() {
        AppUserDto dto = new AppUserDto();
        dto.setUsername("test");

        assertEquals(dto, dto);
    }

    @DisplayName("equals returns false when username differs")
    @Test
    void test_equals_different_username() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("user1");
        dto1.setEmail("same@example.com");

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("user2");
        dto2.setEmail("same@example.com");

        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals returns false when email differs")
    @Test
    void test_equals_different_email() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("same");
        dto1.setEmail("email1@example.com");

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("same");
        dto2.setEmail("email2@example.com");

        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals returns false when permissions differ")
    @Test
    void test_equals_different_permissions() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("user");
        dto1.setPermissions(Set.of("READ_TRANSACTION"));

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("user");
        dto2.setPermissions(Set.of("WRITE_TRANSACTION"));

        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals returns false when roles differ")
    @Test
    void test_equals_different_roles() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("user");
        dto1.setRoles(Set.of("Operator"));

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("user");
        dto2.setRoles(Set.of("Viewer"));

        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals handles null fields correctly")
    @Test
    void test_equals_with_null_fields() {
        AppUserDto dto1 = new AppUserDto();
        AppUserDto dto2 = new AppUserDto();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("hashCode is consistent across multiple calls")
    @Test
    void test_hashcode_consistency() {
        AppUserDto dto = new AppUserDto();
        dto.setUsername("user");
        dto.setEmail("user@example.com");

        int hash1 = dto.hashCode();
        int hash2 = dto.hashCode();
        int hash3 = dto.hashCode();

        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @DisplayName("all-args constructor sets all fields")
    @Test
    void test_all_args_constructor() {
        String email = "test@example.com";
        String username = "testuser";
        String firstname = "Test";
        String lastname = "User";
        boolean accountNonExpired = true;
        boolean accountNonLocked = true;
        boolean credentialsNonExpired = true;
        boolean enabled = true;
        boolean firstTimeLoginRemaining = false;
        boolean deleted = false;
        Date lastTimePasswordUpdated = new Date();
        boolean passwordNeverExpires = false;
        java.util.List<String> payeePartyIds = java.util.List.of("party1", "party2");
        java.util.List<String> currencies = java.util.List.of("USD", "EUR");
        java.util.List<String> payeePartyIdTypes = java.util.List.of("type1", "type2");
        Set<String> permissions = Set.of("READ_TRANSACTION");
        Set<String> roles = Set.of("Operator");

        AppUserDto dto = new AppUserDto(1L, email, username,firstname, lastname, accountNonExpired,
                accountNonLocked, credentialsNonExpired, enabled, firstTimeLoginRemaining, deleted,
                lastTimePasswordUpdated, passwordNeverExpires, payeePartyIds, currencies,
                payeePartyIdTypes, permissions, roles);

        assertEquals(email, dto.getEmail());
        assertEquals(username, dto.getUsername());
        assertEquals(firstname, dto.getFirstname());
        assertEquals(lastname, dto.getLastname());
        assertEquals(accountNonExpired, dto.isAccountNonExpired());
        assertEquals(accountNonLocked, dto.isAccountNonLocked());
        assertEquals(credentialsNonExpired, dto.isCredentialsNonExpired());
        assertEquals(enabled, dto.isEnabled());
        assertEquals(firstTimeLoginRemaining, dto.isFirstTimeLoginRemaining());
        assertEquals(deleted, dto.isDeleted());
        assertEquals(lastTimePasswordUpdated, dto.getLastTimePasswordUpdated());
        assertEquals(passwordNeverExpires, dto.isPasswordNeverExpires());
        assertEquals(payeePartyIds, dto.getPayeePartyIds());
        assertEquals(currencies, dto.getCurrencies());
        assertEquals(payeePartyIdTypes, dto.getPayeePartyIdTypes());
        assertEquals(permissions, dto.getPermissions());
        assertEquals(roles, dto.getRoles());
    }

    // Additional comprehensive coverage tests for @Data annotation

    @DisplayName("equals handles all boolean field combinations")
    @Test
    void test_equals_boolean_fields() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("test");
        dto1.setAccountNonExpired(true);
        dto1.setAccountNonLocked(false);
        dto1.setCredentialsNonExpired(true);
        dto1.setEnabled(false);
        dto1.setFirstTimeLoginRemaining(true);
        dto1.setDeleted(false);
        dto1.setPasswordNeverExpires(true);

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("test");
        dto2.setAccountNonExpired(true);
        dto2.setAccountNonLocked(false);
        dto2.setCredentialsNonExpired(true);
        dto2.setEnabled(false);
        dto2.setFirstTimeLoginRemaining(true);
        dto2.setDeleted(false);
        dto2.setPasswordNeverExpires(true);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        // Test each boolean field difference
        dto2.setAccountNonExpired(false);
        assertNotEquals(dto1, dto2);
        dto2.setAccountNonExpired(true);

        dto2.setAccountNonLocked(true);
        assertNotEquals(dto1, dto2);
        dto2.setAccountNonLocked(false);

        dto2.setCredentialsNonExpired(false);
        assertNotEquals(dto1, dto2);
        dto2.setCredentialsNonExpired(true);

        dto2.setEnabled(true);
        assertNotEquals(dto1, dto2);
        dto2.setEnabled(false);

        dto2.setFirstTimeLoginRemaining(false);
        assertNotEquals(dto1, dto2);
        dto2.setFirstTimeLoginRemaining(true);

        dto2.setDeleted(true);
        assertNotEquals(dto1, dto2);
        dto2.setDeleted(false);

        dto2.setPasswordNeverExpires(false);
        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals handles Date field variations")
    @Test
    void test_equals_date_fields() {
        Date date1 = new Date(1000000L);
        Date date2 = new Date(1000000L);
        Date date3 = new Date(2000000L);

        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("test");
        dto1.setLastTimePasswordUpdated(date1);

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("test");
        dto2.setLastTimePasswordUpdated(date2);

        AppUserDto dto3 = new AppUserDto();
        dto3.setUsername("test");
        dto3.setLastTimePasswordUpdated(date3);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);

        // Test with null dates
        dto1.setLastTimePasswordUpdated(null);
        dto2.setLastTimePasswordUpdated(null);
        assertEquals(dto1, dto2);

        dto2.setLastTimePasswordUpdated(date1);
        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals handles List field variations")
    @Test
    void test_equals_list_fields() {
        java.util.List<String> list1 = java.util.List.of("item1", "item2");
        java.util.List<String> list2 = java.util.List.of("item1", "item2");
        java.util.List<String> list3 = java.util.List.of("item1", "item3");

        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("test");
        dto1.setPayeePartyIds(list1);
        dto1.setCurrencies(list1);
        dto1.setPayeePartyIdTypes(list1);

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("test");
        dto2.setPayeePartyIds(list2);
        dto2.setCurrencies(list2);
        dto2.setPayeePartyIdTypes(list2);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        // Test payeePartyIds difference
        dto2.setPayeePartyIds(list3);
        assertNotEquals(dto1, dto2);
        dto2.setPayeePartyIds(list1);

        // Test currencies difference
        dto2.setCurrencies(list3);
        assertNotEquals(dto1, dto2);
        dto2.setCurrencies(list1);

        // Test payeePartyIdTypes difference
        dto2.setPayeePartyIdTypes(list3);
        assertNotEquals(dto1, dto2);
        dto2.setPayeePartyIdTypes(list1);

        // Test with null lists
        dto1.setPayeePartyIds(null);
        dto2.setPayeePartyIds(null);
        assertEquals(dto1, dto2);

        dto2.setPayeePartyIds(list1);
        assertNotEquals(dto1, dto2);
    }

    @DisplayName("equals handles firstname and lastname variations")
    @Test
    void test_equals_name_fields() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("test");
        dto1.setFirstname("John");
        dto1.setLastname("Doe");

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("test");
        dto2.setFirstname("John");
        dto2.setLastname("Doe");

        assertEquals(dto1, dto2);

        dto2.setFirstname("Jane");
        assertNotEquals(dto1, dto2);
        dto2.setFirstname("John");

        dto2.setLastname("Smith");
        assertNotEquals(dto1, dto2);
        dto2.setLastname("Doe");

        // Test with null names
        dto1.setFirstname(null);
        dto1.setLastname(null);
        dto2.setFirstname(null);
        dto2.setLastname(null);
        assertEquals(dto1, dto2);

        dto2.setFirstname("Test");
        assertNotEquals(dto1, dto2);
    }

    @DisplayName("constructor with AppUser handles null lists correctly")
    @Test
    void test_constructor_with_app_user_null_lists() {
        AppUser appUser = new AppUser();
        appUser.setEmail("test@example.com");
        appUser.setUsername("testuser");
        // Don't set lists - they should be null

        Set<String> permissions = Set.of("READ_TRANSACTION");
        Set<String> roles = Set.of("Operator");

        AppUserDto dto = new AppUserDto(appUser, permissions, roles);

        assertEquals("test@example.com", dto.getEmail());
        assertEquals("testuser", dto.getUsername());
        assertNotNull(dto.getPayeePartyIds());
        assertTrue(dto.getPayeePartyIds().isEmpty());
        assertNotNull(dto.getCurrencies());
        assertTrue(dto.getCurrencies().isEmpty());
        assertNotNull(dto.getPayeePartyIdTypes());
        assertTrue(dto.getPayeePartyIdTypes().isEmpty());
        assertEquals(permissions, dto.getPermissions());
        assertEquals(roles, dto.getRoles());
    }

    @DisplayName("toString contains all non-null field values")
    @Test
    void test_toString_with_all_fields() {
        AppUserDto dto = new AppUserDto();
        dto.setEmail("test@example.com");
        dto.setUsername("testuser");
        dto.setFirstname("Test");
        dto.setLastname("User");
        dto.setAccountNonExpired(true);
        dto.setAccountNonLocked(false);
        dto.setCredentialsNonExpired(true);
        dto.setEnabled(false);
        dto.setFirstTimeLoginRemaining(true);
        dto.setDeleted(false);
        dto.setPasswordNeverExpires(true);
        dto.setLastTimePasswordUpdated(new Date());
        dto.setPayeePartyIds(java.util.List.of("party1"));
        dto.setCurrencies(java.util.List.of("USD"));
        dto.setPayeePartyIdTypes(java.util.List.of("type1"));
        dto.setPermissions(Set.of("READ_TRANSACTION"));
        dto.setRoles(Set.of("Operator"));

        String toString = dto.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("AppUserDto"));
        assertTrue(toString.contains("test@example.com"));
        assertTrue(toString.contains("testuser"));
        assertTrue(toString.contains("Test"));
        assertTrue(toString.contains("User"));
    }

    @DisplayName("equals with mixed null and non-null collections")
    @Test
    void test_equals_mixed_null_collections() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("test");
        dto1.setPermissions(null);
        dto1.setRoles(Set.of("Operator"));
        dto1.setPayeePartyIds(java.util.List.of("party1"));
        dto1.setCurrencies(null);

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("test");
        dto2.setPermissions(null);
        dto2.setRoles(Set.of("Operator"));
        dto2.setPayeePartyIds(java.util.List.of("party1"));
        dto2.setCurrencies(null);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());

        dto2.setPermissions(Set.of("READ"));
        assertNotEquals(dto1, dto2);

        dto2.setPermissions(null);
        dto2.setCurrencies(java.util.List.of("USD"));
        assertNotEquals(dto1, dto2);
    }

    @DisplayName("hashCode varies with different field values")
    @Test
    void test_hashcode_field_variations() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("user1");
        dto1.setEmail("user1@example.com");

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("user2");
        dto2.setEmail("user2@example.com");

        assertNotEquals(dto1.hashCode(), dto2.hashCode());

        dto1.setAccountNonExpired(true);
        dto2.setAccountNonExpired(false);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());

        dto1.setPermissions(Set.of("READ"));
        dto2.setPermissions(Set.of("WRITE"));
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @DisplayName("getters return exact values set by setters")
    @Test
    void test_all_getters_and_setters() {
        AppUserDto dto = new AppUserDto();
        
        // Test all String fields
        dto.setEmail("email@test.com");
        assertEquals("email@test.com", dto.getEmail());
        
        dto.setUsername("username");
        assertEquals("username", dto.getUsername());
        
        dto.setFirstname("first");
        assertEquals("first", dto.getFirstname());
        
        dto.setLastname("last");
        assertEquals("last", dto.getLastname());

        // Test all boolean fields
        dto.setAccountNonExpired(true);
        assertTrue(dto.isAccountNonExpired());
        dto.setAccountNonExpired(false);
        assertFalse(dto.isAccountNonExpired());

        dto.setAccountNonLocked(true);
        assertTrue(dto.isAccountNonLocked());
        dto.setAccountNonLocked(false);
        assertFalse(dto.isAccountNonLocked());

        dto.setCredentialsNonExpired(true);
        assertTrue(dto.isCredentialsNonExpired());
        dto.setCredentialsNonExpired(false);
        assertFalse(dto.isCredentialsNonExpired());

        dto.setEnabled(true);
        assertTrue(dto.isEnabled());
        dto.setEnabled(false);
        assertFalse(dto.isEnabled());

        dto.setFirstTimeLoginRemaining(true);
        assertTrue(dto.isFirstTimeLoginRemaining());
        dto.setFirstTimeLoginRemaining(false);
        assertFalse(dto.isFirstTimeLoginRemaining());

        dto.setDeleted(true);
        assertTrue(dto.isDeleted());
        dto.setDeleted(false);
        assertFalse(dto.isDeleted());

        dto.setPasswordNeverExpires(true);
        assertTrue(dto.isPasswordNeverExpires());
        dto.setPasswordNeverExpires(false);
        assertFalse(dto.isPasswordNeverExpires());

        // Test Date field
        Date date = new Date();
        dto.setLastTimePasswordUpdated(date);
        assertEquals(date, dto.getLastTimePasswordUpdated());

        // Test List fields
        java.util.List<String> payeeIds = java.util.List.of("id1", "id2");
        dto.setPayeePartyIds(payeeIds);
        assertEquals(payeeIds, dto.getPayeePartyIds());

        java.util.List<String> currencies = java.util.List.of("USD", "EUR");
        dto.setCurrencies(currencies);
        assertEquals(currencies, dto.getCurrencies());

        java.util.List<String> idTypes = java.util.List.of("type1", "type2");
        dto.setPayeePartyIdTypes(idTypes);
        assertEquals(idTypes, dto.getPayeePartyIdTypes());

        // Test Set fields
        Set<String> permissions = Set.of("READ", "WRITE");
        dto.setPermissions(permissions);
        assertEquals(permissions, dto.getPermissions());

        Set<String> roles = Set.of("USER", "ADMIN");
        dto.setRoles(roles);
        assertEquals(roles, dto.getRoles());
    }

    @DisplayName("equals handles empty vs null collections differently")
    @Test
    void test_equals_empty_vs_null_collections() {
        AppUserDto dto1 = new AppUserDto();
        dto1.setUsername("test");
        dto1.setPermissions(Set.of());
        dto1.setRoles(Set.of());
        dto1.setPayeePartyIds(java.util.List.of());
        dto1.setCurrencies(java.util.List.of());
        dto1.setPayeePartyIdTypes(java.util.List.of());

        AppUserDto dto2 = new AppUserDto();
        dto2.setUsername("test");
        dto2.setPermissions(null);
        dto2.setRoles(null);
        dto2.setPayeePartyIds(null);
        dto2.setCurrencies(null);
        dto2.setPayeePartyIdTypes(null);

        assertNotEquals(dto1, dto2);
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }
}
