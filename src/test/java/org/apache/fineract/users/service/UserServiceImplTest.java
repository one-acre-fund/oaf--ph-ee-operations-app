package org.apache.fineract.users.service;

import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.apache.fineract.organisation.user.AppUserDto;
import org.apache.fineract.organisation.user.AppUserUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserServiceImpl(appUserRepository);
    }

    @DisplayName("retrieveUserByUsername returns null and sets 404 when user does not exist")
    @Test
    void test_retrieve_user_not_found() {
        String username = "missing.user@oneacrefund.org";
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(appUserRepository.findAppUserByName(username)).thenReturn(null);

        AppUserDto result = userService.retrieveUserByUsername(username, response);

        assertNull(result);
        assertEquals(MockHttpServletResponse.SC_NOT_FOUND, response.getStatus());
        verify(appUserRepository).findAppUserByName(username);
    }

    @DisplayName("retrieveUserByUsername aggregates active role permissions and deduplicates shared permissions")
    @Test
    void test_retrieve_user_aggregates_permissions_from_active_roles_only() {
        String username = "kelvin.thuku@oneacrefund.org";
        MockHttpServletResponse response = new MockHttpServletResponse();

        Permission readTxn = permission("READ_TRANSACTION");
        Permission writeTxn = permission("WRITE_TRANSACTION");
        Permission adminAccess = permission("ADMIN_ACCESS");

        Role operator = role("Operator", false, readTxn, writeTxn);
        Role viewer = role("Viewer", false, readTxn);
        Role superAdmin = role("SuperAdmin", true, adminAccess);

        AppUser user = new AppUser();
        user.setUsername("kelvin.thuku");
        user.setEmail(username);
        user.setFirstname("Kelvin");
        user.setLastname("Thuku");
        user.setPassword("secret");
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        user.setFirstTimeLoginRemaining(false);
        user.setDeleted(false);
        user.setLastTimePasswordUpdated(new Date());
        user.setPasswordNeverExpires(false);
        user.setRoles(Arrays.asList(operator, viewer, superAdmin));

        when(appUserRepository.findAppUserByName(username)).thenReturn(user);

        AppUserDto result = userService.retrieveUserByUsername(username, response);

        assertNotNull(result);
        assertEquals("kelvin.thuku", result.getUsername());
        assertEquals(username, result.getEmail());
        assertEquals("Kelvin", result.getFirstname());
        assertEquals("Thuku", result.getLastname());
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains("Operator"));
        assertTrue(result.getRoles().contains("Viewer"));
        assertEquals(2, result.getPermissions().size());
        assertTrue(result.getPermissions().contains("READ_TRANSACTION"));
        assertTrue(result.getPermissions().contains("WRITE_TRANSACTION"));
    }

    @DisplayName("retrieveUserByUsername returns empty permission and role sets when user has no roles")
    @Test
    void test_retrieve_user_with_no_roles() {
        String username = "no.roles@oneacrefund.org";
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser user = new AppUser();
        user.setUsername("no.roles");
        user.setEmail(username);
        user.setRoles(Collections.emptyList());

        when(appUserRepository.findAppUserByName(username)).thenReturn(user);

        AppUserDto result = userService.retrieveUserByUsername(username, response);

        assertNotNull(result);
        assertNotNull(result.getPermissions());
        assertNotNull(result.getRoles());
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getRoles().isEmpty());
    }

    @DisplayName("retrieveUserByUsername handles user with null roles correctly")
    @Test
    void test_retrieve_user_with_null_roles() {
        String username = "null.roles@oneacrefund.org";
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser user = new AppUser();
        user.setUsername("null.roles");
        user.setEmail(username);
        user.setRoles(null); // Explicitly set to null

        when(appUserRepository.findAppUserByName(username)).thenReturn(user);

        AppUserDto result = userService.retrieveUserByUsername(username, response);

        assertNotNull(result);
        assertNotNull(result.getPermissions());
        assertNotNull(result.getRoles());
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getRoles().isEmpty());
    }

    @DisplayName("retrieveUserById returns null and sets 404 when user does not exist")
    @Test
    void test_retrieve_user_by_id_not_found() {
        Long userId = 999L;
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        AppUserDto result = userService.retrieveUserById(userId, response);

        assertNull(result);
        assertEquals(MockHttpServletResponse.SC_NOT_FOUND, response.getStatus());
        verify(appUserRepository).findById(userId);
    }

    @DisplayName("retrieveUserById returns user with permissions when user exists")
    @Test
    void test_retrieve_user_by_id_found() {
        Long userId = 1L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        Permission readTxn = permission("READ_TRANSACTION");
        Permission writeTxn = permission("WRITE_TRANSACTION");

        Role operator = role("Operator", false, readTxn, writeTxn);
        Role viewer = role("Viewer", false, readTxn);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("test.user");
        user.setEmail("test.user@oneacrefund.org");
        user.setFirstname("Test");
        user.setLastname("User");
        user.setPassword("secret");
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setEnabled(true);
        user.setFirstTimeLoginRemaining(false);
        user.setDeleted(false);
        user.setLastTimePasswordUpdated(new Date());
        user.setPasswordNeverExpires(false);
        user.setRoles(Arrays.asList(operator, viewer));

        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        AppUserDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals("test.user", result.getUsername());
        assertEquals("test.user@oneacrefund.org", result.getEmail());
        assertEquals("Test", result.getFirstname());
        assertEquals("User", result.getLastname());
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains("Operator"));
        assertTrue(result.getRoles().contains("Viewer"));
        assertEquals(2, result.getPermissions().size());
        assertTrue(result.getPermissions().contains("READ_TRANSACTION"));
        assertTrue(result.getPermissions().contains("WRITE_TRANSACTION"));
        verify(appUserRepository).findById(userId);
    }

    @DisplayName("retrieveUserById excludes permissions from disabled roles")
    @Test
    void test_retrieve_user_by_id_excludes_disabled_roles() {
        Long userId = 2L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        Permission activePerm = permission("READ_TRANSACTION");
        Permission disabledPerm = permission("ADMIN_ACCESS");

        Role activeRole = role("Operator", false, activePerm);
        Role disabledRole = role("SuperAdmin", true, disabledPerm);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("active.user");
        user.setEmail("active.user@oneacrefund.org");
        user.setRoles(Arrays.asList(activeRole, disabledRole));

        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        AppUserDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains("Operator"));
        assertEquals(1, result.getPermissions().size());
        assertTrue(result.getPermissions().contains("READ_TRANSACTION"));
        assertFalse(result.getPermissions().contains("ADMIN_ACCESS"));
        verify(appUserRepository).findById(userId);
    }

    @DisplayName("retrieveUserById returns empty permissions and roles when user has no roles")
    @Test
    void test_retrieve_user_by_id_with_no_roles() {
        Long userId = 3L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("no.roles");
        user.setEmail("no.roles@oneacrefund.org");
        user.setRoles(Collections.emptyList());

        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        AppUserDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
        assertNotNull(result.getPermissions());
        assertNotNull(result.getRoles());
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getRoles().isEmpty());
        verify(appUserRepository).findById(userId);
    }

    @DisplayName("retrieveUserById deduplicates permissions shared across multiple active roles")
    @Test
    void test_retrieve_user_by_id_deduplicates_permissions() {
        Long userId = 4L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        Permission sharedPerm = permission("READ_TRANSACTION");
        Permission uniquePerm = permission("WRITE_TRANSACTION");

        Role role1 = role("Operator", false, sharedPerm, uniquePerm);
        Role role2 = role("Viewer", false, sharedPerm);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("shared.perms");
        user.setEmail("shared.perms@oneacrefund.org");
        user.setRoles(Arrays.asList(role1, role2));

        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        AppUserDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(2, result.getRoles().size());
        assertTrue(result.getRoles().contains("Operator"));
        assertTrue(result.getRoles().contains("Viewer"));
        assertEquals(2, result.getPermissions().size(), "READ_TRANSACTION should appear only once");
        assertTrue(result.getPermissions().contains("READ_TRANSACTION"));
        assertTrue(result.getPermissions().contains("WRITE_TRANSACTION"));
        verify(appUserRepository).findById(userId);
    }

    @DisplayName("retrieveUserById handles user with null roles correctly")
    @Test
    void test_retrieve_user_by_id_with_null_roles() {
        Long userId = 5L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser user = new AppUser();
        user.setId(userId);
        user.setUsername("null.roles");
        user.setEmail("null.roles@oneacrefund.org");
        user.setRoles(null); // Explicitly set to null

        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        AppUserDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
        assertNotNull(result.getPermissions());
        assertNotNull(result.getRoles());
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getRoles().isEmpty());
        verify(appUserRepository).findById(userId);
    }

    @DisplayName("updateUser successfully updates provided fields only")
    @Test
    void test_update_user_partial_update() {
        Long userId = 1L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser existingUser = new AppUser();
        existingUser.setId(userId);
        existingUser.setUsername("oldusername");
        existingUser.setFirstname("OldFirst");
        existingUser.setLastname("OldLast");
        existingUser.setEmail("old@example.com");
        existingUser.setPassword("oldpassword");
        existingUser.setEnabled(false);

        AppUserUpdateDto updateDto = new AppUserUpdateDto();
        updateDto.setFirstname("NewFirst");
        // Note: lastname not set - should remain unchanged

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenReturn(existingUser);

        boolean result = userService.updateUser(userId, updateDto, response);

        assertTrue(result);
        assertEquals("NewFirst", existingUser.getFirstname());
        // Unchanged fields
        assertEquals("OldLast", existingUser.getLastname());
        assertEquals("old@example.com", existingUser.getEmail());
        assertEquals("oldusername", existingUser.getUsername());
        assertEquals("oldpassword", existingUser.getPassword());
        assertFalse(existingUser.isEnabled()); // Should remain unchanged
        verify(appUserRepository).saveAndFlush(existingUser);
    }

    @DisplayName("updateUser returns false and sets 404 when user not found")
    @Test
    void test_update_user_not_found() {
        Long userId = 999L;
        MockHttpServletResponse response = new MockHttpServletResponse();
        AppUserUpdateDto updateDto = new AppUserUpdateDto();
        updateDto.setFirstname("NewFirst");

        when(appUserRepository.findById(userId)).thenReturn(Optional.empty());

        boolean result = userService.updateUser(userId, updateDto, response);

        assertFalse(result);
        assertEquals(MockHttpServletResponse.SC_NOT_FOUND, response.getStatus());
        verify(appUserRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @DisplayName("updateUser updates lastname correctly")
    @Test
    void test_update_user_lastname() {
        Long userId = 1L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser existingUser = new AppUser();
        existingUser.setId(userId);
        existingUser.setFirstname("OldFirst");
        existingUser.setLastname("OldLast");

        AppUserUpdateDto updateDto = new AppUserUpdateDto();
        updateDto.setLastname("NewLast");

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenReturn(existingUser);

        boolean result = userService.updateUser(userId, updateDto, response);

        assertTrue(result);
        assertEquals("NewLast", existingUser.getLastname());
        assertEquals("OldFirst", existingUser.getFirstname()); // Should remain unchanged
        verify(appUserRepository).saveAndFlush(existingUser);
    }

    @DisplayName("updateUser updates both firstname and lastname when both are provided")
    @Test
    void test_update_user_both_names() {
        Long userId = 1L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser existingUser = new AppUser();
        existingUser.setId(userId);
        existingUser.setFirstname("OldFirst");
        existingUser.setLastname("OldLast");

        AppUserUpdateDto updateDto = new AppUserUpdateDto();
        updateDto.setFirstname("NewFirst");
        updateDto.setLastname("NewLast");

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenReturn(existingUser);

        boolean result = userService.updateUser(userId, updateDto, response);

        assertTrue(result);
        assertEquals("NewFirst", existingUser.getFirstname());
        assertEquals("NewLast", existingUser.getLastname());
        verify(appUserRepository).saveAndFlush(existingUser);
    }

    @DisplayName("updateUser handles null values correctly (no update)")
    @Test
    void test_update_user_null_values() {
        Long userId = 1L;
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUser existingUser = new AppUser();
        existingUser.setId(userId);
        existingUser.setFirstname("OldFirst");
        existingUser.setLastname("OldLast");

        AppUserUpdateDto updateDto = new AppUserUpdateDto();
        // Both firstname and lastname are null - should not update anything

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenReturn(existingUser);

        boolean result = userService.updateUser(userId, updateDto, response);

        assertTrue(result);
        assertEquals("OldFirst", existingUser.getFirstname()); // Should remain unchanged
        assertEquals("OldLast", existingUser.getLastname()); // Should remain unchanged
        verify(appUserRepository).saveAndFlush(existingUser);
    }

    private Permission permission(String code) {
        Permission permission = new Permission();
        permission.setCode(code);
        return permission;
    }

    private Role role(String name, boolean disabled, Permission... permissions) {
        Role role = new Role();
        role.setName(name);
        role.setDisabled(disabled);
        role.setPermissions(Arrays.asList(permissions));
        return role;
    }
}

