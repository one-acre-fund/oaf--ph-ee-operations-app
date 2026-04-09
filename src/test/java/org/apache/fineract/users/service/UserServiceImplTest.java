package org.apache.fineract.users.service;

import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.apache.fineract.organisation.user.UserPermissionsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        UserPermissionsDto result = userService.retrieveUserByUsername(username, response);

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

        UserPermissionsDto result = userService.retrieveUserByUsername(username, response);

        assertNotNull(result);
        assertEquals("kelvin.thuku", result.getAppUser().getUsername());
        assertEquals(username, result.getAppUser().getEmail());
        assertEquals("Kelvin", result.getAppUser().getFirstname());
        assertEquals("Thuku", result.getAppUser().getLastname());
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

        UserPermissionsDto result = userService.retrieveUserByUsername(username, response);

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

        UserPermissionsDto result = userService.retrieveUserByUsername(username, response);

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

        UserPermissionsDto result = userService.retrieveUserById(userId, response);

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

        UserPermissionsDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(user, result.getAppUser());
        assertEquals("test.user", result.getAppUser().getUsername());
        assertEquals("test.user@oneacrefund.org", result.getAppUser().getEmail());
        assertEquals("Test", result.getAppUser().getFirstname());
        assertEquals("User", result.getAppUser().getLastname());
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

        UserPermissionsDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().contains("Operator"));
        assertEquals(1, result.getPermissions().size());
        assertTrue(result.getPermissions().contains("READ_TRANSACTION"));
        assertTrue(!result.getPermissions().contains("ADMIN_ACCESS"));
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

        UserPermissionsDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(user, result.getAppUser());
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

        UserPermissionsDto result = userService.retrieveUserById(userId, response);

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

        UserPermissionsDto result = userService.retrieveUserById(userId, response);

        assertNotNull(result);
        assertEquals(user, result.getAppUser());
        assertNotNull(result.getPermissions());
        assertNotNull(result.getRoles());
        assertTrue(result.getPermissions().isEmpty());
        assertTrue(result.getRoles().isEmpty());
        verify(appUserRepository).findById(userId);
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

