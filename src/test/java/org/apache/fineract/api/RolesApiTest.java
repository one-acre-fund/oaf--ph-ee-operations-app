package org.apache.fineract.api;

import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.permission.PermissionData;
import org.apache.fineract.organisation.permission.PermissionRepository;
import org.apache.fineract.organisation.role.PermissionsCommand;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RolePermissionsData;
import org.apache.fineract.organisation.role.RoleRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

import java.util.*;
import java.util.stream.Collectors;

class RolesApiTest {
    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AppUser connectedUser;

    @InjectMocks
    RolesApi rolesApi;

    private MockedStatic<ThreadLocalContextUtil> mockedThreadLocalContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockedThreadLocalContext = Mockito.mockStatic(ThreadLocalContextUtil.class);
        mockedThreadLocalContext.when(ThreadLocalContextUtil::getCurrentUser).thenReturn(connectedUser);
    }

    @AfterEach
    void tearDown() {
        if (mockedThreadLocalContext != null) {
            mockedThreadLocalContext.close();
        }
    }

    @Test
    void test_retrieve_all_roles_successfully() {
        List<Role> roles = Arrays.asList(new Role(), new Role());
        Mockito.when(roleRepository.findAll()).thenReturn(roles);

        List<Role> result = rolesApi.retrieveAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        Mockito.verify(roleRepository, Mockito.times(1)).findAll();
    }

    @Test
    void test_retrieve_existing_role() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role expectedRole = new Role();
        expectedRole.setId(1L);
        expectedRole.setName("Admin");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(expectedRole));

        Role result = rolesApi.retrieveOne(1L, response);

        assertNotNull(result);
        assertEquals(expectedRole.getId(), result.getId());
        assertEquals(expectedRole.getName(), result.getName());
    }
    @DisplayName("Retrieve permissions for an existing role")
    @Test
    void test_retrieve_permissions_existing_role() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setName("Admin");
        role.setDescription("Administrator role");
        role.setDisabled(false);

        // Create sample PermissionData - some selected, some not
        PermissionData permission1 = new PermissionData("authorization", "READ_USER", "USER", "READ", true);
        PermissionData permission2 = new PermissionData("authorization", "CREATE_USER", "USER", "CREATE", true);
        PermissionData permission3 = new PermissionData("authorization", "UPDATE_USER", "USER", "UPDATE", false);
        PermissionData permission4 = new PermissionData("authorization", "DELETE_USER", "USER", "DELETE", false);
        
        List<PermissionData> allPermissions = Arrays.asList(permission1, permission2, permission3, permission4);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        Mockito.when(permissionRepository.findAllPermissionsWithRoleSelection(roleId)).thenReturn(allPermissions);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        RolePermissionsData result = rolesApi.retrievePermissions(roleId, response);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.getPermissionUsageData().size());
        
        // Verify selected permissions
        List<PermissionData> selectedPermissions = result.getPermissionUsageData().stream()
                .filter(PermissionData::getSelected)
                .collect(Collectors.toList());
        Assertions.assertEquals(2, selectedPermissions.size());
        
        // Verify non-selected permissions
        List<PermissionData> nonSelectedPermissions = result.getPermissionUsageData().stream()
                .filter(p -> !p.getSelected())
                .collect(Collectors.toList());
        Assertions.assertEquals(2, nonSelectedPermissions.size());
        
        // Verify the repository method was called
        Mockito.verify(permissionRepository, Mockito.times(1)).findAllPermissionsWithRoleSelection(roleId);
    }

    @Test
    @DisplayName("Retrieve permissions - role not found returns 404")
    void test_retrieve_permissions_role_not_found() {
        // Arrange
        Long roleId = 999L;
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // Act
        RolePermissionsData result = rolesApi.retrievePermissions(roleId, response);

        // Assert
        Assertions.assertNull(result);
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
        Mockito.verify(permissionRepository, Mockito.never()).findAllPermissionsWithRoleSelection(Mockito.any());
    }

    @Test
    @DisplayName("Retrieve permissions - permission check failure throws exception")
    void test_retrieve_permissions_no_read_permission() {
        // Arrange
        Long roleId = 1L;
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.doThrow(new RuntimeException("User does not have READ permission"))
                .when(connectedUser).validateHasReadPermission("ROLE");

        // Act & Assert
        Assertions.assertThrows(RuntimeException.class, () -> {
            rolesApi.retrievePermissions(roleId, response);
        });

        Mockito.verify(roleRepository, Mockito.never()).findById(Mockito.any());
    }

    @Test
    void test_create_role_with_unique_name() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role newRole = new Role();
        newRole.setName("uniqueRoleName");

        when(roleRepository.getRoleByName("uniqueRoleName")).thenReturn(null);

        rolesApi.create(newRole, response);

        verify(roleRepository, times(1)).saveAndFlush(newRole);
        verify(response, never()).setStatus(HttpServletResponse.SC_CONFLICT);
    }
    @Test
    void test_create_role_with_existing_name() {
        HttpServletResponse response = mock(HttpServletResponse.class);

        Role existingRole = new Role();
        existingRole.setName("existingrolename"); // Correct normalized name

        Role newRole = new Role();
        newRole.setName("existingRoleName"); // Will be normalized to "existingrolename"

        when(roleRepository.getRoleByName("existingrolename")).thenReturn(existingRole); // Correct normalized lookup

        rolesApi.create(newRole, response);

        verify(roleRepository, never()).saveAndFlush(any(Role.class)); // Use any(Role.class) since name gets modified
        verify(response, times(1)).setStatus(HttpServletResponse.SC_CONFLICT);
    }

    @DisplayName("Update an existing role with valid roleId and role data")
    @Test
    void update_existing_role_with_valid_data() {
        // Arrange
        Role existingRole = new Role();
        existingRole.setId(1L);
        existingRole.setName("Admin");
        existingRole.setDescription("Administrator role");
        existingRole.setDisabled(false);
        Mockito.when(roleRepository.findById(1L)).thenReturn(Optional.of(existingRole));

        Role updatedRole = new Role();
        updatedRole.setName("Admin Updated");
        updatedRole.setDescription("Updated Administrator role");
        updatedRole.setDisabled(false);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.update(1L, updatedRole, response);

        // Assert
        Mockito.verify(roleRepository).saveAndFlush(updatedRole);
        Assertions.assertEquals(existingRole.getAppusers(), updatedRole.getAppusers());
        Assertions.assertEquals(existingRole.getPermissions(), updatedRole.getPermissions());
    }

    @DisplayName("Delete an existing role by ID")
    @Test
    void delete_existing_role_by_id() {
        // Arrange
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Long roleId = 1L;

        Role role = new Role();
        role.setId(roleId);
        role.setPermissions(new ArrayList<>());
        role.setAppUsers(new ArrayList<>());

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        // Act
        rolesApi.delete(roleId, response);

        // Assert
        Mockito.verify(roleRepository, Mockito.times(1)).saveAndFlush(role);
        Mockito.verify(roleRepository, Mockito.times(1)).deleteById(roleId);
        Mockito.verify(response, Mockito.never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Attempt to delete a role with a null ID")
    @Test
    void delete_role_with_null_id() {
        // Arrange
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Long roleId = null;

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // Act
        rolesApi.delete(roleId, response);

        // Assert
        Mockito.verify(roleRepository, Mockito.never()).deleteById(Mockito.any());
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Delete role clears permissions and appUsers before deletion")
    @Test
    void delete_role_clears_relationships() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);

        Permission permission1 = new Permission();
        permission1.setId(1L);
        Permission permission2 = new Permission();
        permission2.setId(2L);
        
        Collection<Permission> permissions = new ArrayList<>(Arrays.asList(permission1, permission2));
        role.setPermissions(permissions);
        role.setAppUsers(new ArrayList<>());

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.delete(roleId, response);

        // Assert
        assertEquals(0, role.getPermissions().size());
        assertEquals(0, role.getAppusers().size());
        Mockito.verify(roleRepository, Mockito.times(1)).saveAndFlush(role);
        Mockito.verify(roleRepository, Mockito.times(1)).deleteById(roleId);
    }

    @DisplayName("Permission assignment - no update permission throws exception")
    @Test
    void test_permission_assignment_no_update_permission() {
        // Arrange
        Long roleId = 1L;
        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("READ_USER", true);
        PermissionsCommand command = new PermissionsCommand(permissionsMap);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Mockito.doThrow(new RuntimeException("User does not have UPDATE permission"))
                .when(connectedUser).validateHasUpdatePermission("ROLE");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            rolesApi.permissionAssignment(roleId, command, response);
        });

        verify(roleRepository, never()).findById(Mockito.any());
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
    }

    @DisplayName("Permission assignment - empty permissions map does not save")
    @Test
    void test_permission_assignment_empty_map() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setPermissions(new ArrayList<>());

        Map<String, Boolean> permissionsMap = new HashMap<>(); // Empty map
        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findAll()).thenReturn(Collections.emptyList());

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert
        verify(roleRepository, never()).saveAndFlush(existingRole);
    }

    @DisplayName("Assign new permissions to a role successfully")
    @Test
    void test_assign_new_permissions_to_role() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setPermissions(new ArrayList<>());

        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");
        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        List<Permission> allPermissions = Arrays.asList(permission1, permission2);

        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("READ_USER", true);
        permissionsMap.put("CREATE_USER", true);
        
        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert
        assertEquals(2, existingRole.getPermissions().size());
        assertTrue(existingRole.getPermissions().contains(permission1));
        assertTrue(existingRole.getPermissions().contains(permission2));
        verify(roleRepository, times(1)).saveAndFlush(existingRole);
    }

    @DisplayName("Remove permissions from a role successfully")
    @Test
    void test_remove_permissions_from_role() {
        // Arrange
        Long roleId = 1L;
        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");
        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        Role existingRole = new Role();
        existingRole.setId(roleId);
        Collection<Permission> rolePermissions = new ArrayList<>();
        rolePermissions.add(permission1);
        rolePermissions.add(permission2);
        existingRole.setPermissions(rolePermissions);

        List<Permission> allPermissions = Arrays.asList(permission1, permission2);

        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("READ_USER", false);
        permissionsMap.put("CREATE_USER", false);
        
        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert
        assertEquals(0, existingRole.getPermissions().size());
        verify(roleRepository, times(1)).saveAndFlush(existingRole);
    }

    @DisplayName("Mix of adding and removing permissions from a role")
    @Test
    void test_mixed_permission_assignment() {
        // Arrange
        Long roleId = 1L;
        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");
        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");
        Permission permission3 = new Permission();
        permission3.setCode("UPDATE_USER");

        Role existingRole = new Role();
        existingRole.setId(roleId);
        Collection<Permission> rolePermissions = new ArrayList<>();
        rolePermissions.add(permission1); // Already has READ_USER
        existingRole.setPermissions(rolePermissions);

        List<Permission> allPermissions = Arrays.asList(permission1, permission2, permission3);

        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("READ_USER", false);  // Remove existing
        permissionsMap.put("CREATE_USER", true);  // Add new
        permissionsMap.put("UPDATE_USER", true);  // Add new
        
        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert
        assertEquals(2, existingRole.getPermissions().size());
        assertFalse(existingRole.getPermissions().contains(permission1));
        assertTrue(existingRole.getPermissions().contains(permission2));
        assertTrue(existingRole.getPermissions().contains(permission3));
        verify(roleRepository, times(1)).saveAndFlush(existingRole);
    }

    @DisplayName("No changes when permissions already in desired state")
    @Test
    void test_no_permission_changes() {
        // Arrange
        Long roleId = 1L;
        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");
        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        Role existingRole = new Role();
        existingRole.setId(roleId);
        Collection<Permission> rolePermissions = new ArrayList<>();
        rolePermissions.add(permission1);
        existingRole.setPermissions(rolePermissions);

        List<Permission> allPermissions = Arrays.asList(permission1, permission2);

        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("READ_USER", true);   // Already assigned
        permissionsMap.put("CREATE_USER", false); // Already not assigned
        
        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert
        assertEquals(1, existingRole.getPermissions().size());
        verify(roleRepository, never()).saveAndFlush(existingRole);
    }

    @DisplayName("Permission assignment when role does not exist")
    @Test
    void test_permission_assignment_role_not_found() {
        // Arrange
        Long roleId = 999L;
        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("READ_USER", true);

        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert
        verify(response, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
        verify(permissionRepository, never()).findAll();
    }

    @DisplayName("Permission assignment handles unknown permission codes gracefully")
    @Test
    void test_permission_assignment_with_unknown_code() {
        // Arrange
        Long roleId = 1L;
        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");

        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setPermissions(new ArrayList<>());

        List<Permission> allPermissions = Arrays.asList(permission1);

        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("READ_USER", true);
        permissionsMap.put("UNKNOWN_PERMISSION", true); // This doesn't exist
        
        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert - only valid permission should be added
        assertEquals(1, existingRole.getPermissions().size());
        assertTrue(existingRole.getPermissions().contains(permission1));
        verify(roleRepository, times(1)).saveAndFlush(existingRole);
    }

    @DisplayName("Permission assignment is case insensitive for permission codes")
    @Test
    void test_permission_assignment_case_insensitive() {
        // Arrange
        Long roleId = 1L;
        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");

        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setPermissions(new ArrayList<>());

        List<Permission> allPermissions = Arrays.asList(permission1);

        Map<String, Boolean> permissionsMap = new HashMap<>();
        permissionsMap.put("read_user", true); // lowercase
        
        PermissionsCommand command = new PermissionsCommand(permissionsMap);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, command, response);

        // Assert
        assertEquals(1, existingRole.getPermissions().size());
        assertTrue(existingRole.getPermissions().contains(permission1));
        verify(roleRepository, times(1)).saveAndFlush(existingRole);
    }

    @DisplayName("Disable an enabled role successfully")
    @Test
    void test_disable_enabled_role() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setName("Admin");
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "disable", response);

        // Assert
        assertTrue(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
        verify(response, never()).setStatus(Mockito.anyInt());
    }

    @DisplayName("Enable a disabled role successfully")
    @Test
    void test_enable_disabled_role() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setName("User");
        role.setDisabled(true);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "enable", response);

        // Assert
        assertFalse(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
        verify(response, never()).setStatus(Mockito.anyInt());
    }

    @DisplayName("Disable command is case insensitive")
    @Test
    void test_disable_command_case_insensitive() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "DISABLE", response);

        // Assert
        assertTrue(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
    }

    @DisplayName("Enable command is case insensitive")
    @Test
    void test_enable_command_case_insensitive() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(true);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "ENABLE", response);

        // Assert
        assertFalse(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
    }

    @DisplayName("Disable an already disabled role does not cause error")
    @Test
    void test_disable_already_disabled_role() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(true);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "disable", response);

        // Assert
        assertTrue(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
        verify(response, never()).setStatus(Mockito.anyInt());
    }

    @DisplayName("Enable an already enabled role does not cause error")
    @Test
    void test_enable_already_enabled_role() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "enable", response);

        // Assert
        assertFalse(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
        verify(response, never()).setStatus(Mockito.anyInt());
    }

    @DisplayName("Update role status with non-existent role returns 404")
    @Test
    void test_update_status_role_not_found() {
        // Arrange
        Long roleId = 999L;

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "disable", response);

        // Assert
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
        verify(response, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Update role status with invalid command returns 400")
    @Test
    void test_update_status_invalid_command() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "invalid", response);

        // Assert
        assertFalse(role.getDisabled()); // Should not change
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @DisplayName("Update role status with null command returns 400")
    @Test
    void test_update_status_null_command() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, null, response);

        // Assert
        assertFalse(role.getDisabled());
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @DisplayName("Update role status with empty command returns 400")
    @Test
    void test_update_status_empty_command() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "", response);

        // Assert
        assertFalse(role.getDisabled());
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @DisplayName("Update role status with whitespace command returns 400")
    @Test
    void test_update_status_whitespace_command() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "   ", response);

        // Assert
        assertFalse(role.getDisabled());
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @DisplayName("Disable command with mixed case works correctly")
    @Test
    void test_disable_mixed_case_command() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "DiSaBlE", response);

        // Assert
        assertTrue(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
    }

    @DisplayName("Enable command with mixed case works correctly")
    @Test
    void test_enable_mixed_case_command() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(true);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "EnAbLe", response);

        // Assert
        assertFalse(role.getDisabled());
        verify(roleRepository, times(1)).saveAndFlush(role);
    }

    @DisplayName("Update role - role not found returns 404")
    @Test
    void test_update_role_not_found() {
        // Arrange
        Long roleId = 999L;
        Role updatedRole = new Role();
        updatedRole.setName("New Role Name");
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(response, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
    }

    @DisplayName("Update role - no update permission throws exception")
    @Test
    void test_update_role_no_update_permission() {
        // Arrange
        Long roleId = 1L;
        Role updatedRole = new Role();
        updatedRole.setName("New Role Name");
        
        HttpServletResponse response = mock(HttpServletResponse.class);

        Mockito.doThrow(new RuntimeException("User does not have UPDATE permission"))
                .when(connectedUser).validateHasUpdatePermission("ROLE");

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            rolesApi.update(roleId, updatedRole, response);
        });

        verify(roleRepository, never()).findById(Mockito.any());
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
    }

    @DisplayName("Update role with duplicate name for different role returns 400")
    @Test
    void test_update_role_with_duplicate_name() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("originalrole");
        
        Role anotherRole = new Role();
        anotherRole.setId(2L);
        anotherRole.setName("duplicatename");
        
        Role updatedRole = new Role();
        updatedRole.setName("Duplicate Name"); // Will normalize to "duplicatename"
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("duplicatename")).thenReturn(anotherRole); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
    }

    @DisplayName("Update role with same name as itself succeeds")
    @Test
    void test_update_role_with_same_name() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("SameName");
        existingRole.setDescription("Original description");
        
        Role updatedRole = new Role();
        updatedRole.setName("SameName"); // Same name as existing
        updatedRole.setDescription("Updated description");
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("SameName")).thenReturn(existingRole); // Returns the same role

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(roleId, updatedRole.getId());
        verify(response, never()).setStatus(Mockito.anyInt());
    }

    @DisplayName("Update role preserves existing relationships")
    @Test
    void test_update_role_preserves_relationships() {
        // Arrange
        Long roleId = 1L;
        
        // Create existing permissions
        Permission perm1 = new Permission();
        perm1.setCode("READ_USER");
        Permission perm2 = new Permission();
        perm2.setCode("WRITE_USER");
        Collection<Permission> existingPermissions = Arrays.asList(perm1, perm2);
        
        // Create existing app users
        AppUser user1 = new AppUser();
        user1.setUsername("user1");
        AppUser user2 = new AppUser();
        user2.setUsername("user2");
        Collection<AppUser> existingUsers = Arrays.asList(user1, user2);
        
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("ExistingRole");
        existingRole.setPermissions(existingPermissions);
        existingRole.setAppUsers(existingUsers);
        
        Role updatedRole = new Role();
        updatedRole.setName("UpdatedRole");
        updatedRole.setDescription("Updated description");
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("updatedrole")).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(roleId, updatedRole.getId());
        assertEquals(existingPermissions, updatedRole.getPermissions());
        assertEquals("updatedrole", updatedRole.getName()); // Normalized name
        assertEquals("Updated description", updatedRole.getDescription());
    }

    @DisplayName("Update role with empty name to existing empty name conflict")
    @Test
    void test_update_role_with_empty_name_conflict() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("ExistingRole");
        
        Role anotherRole = new Role();
        anotherRole.setId(2L);
        anotherRole.setName("");
        
        Role updatedRole = new Role();
        updatedRole.setName(""); // Empty name that conflicts with another role
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("")).thenReturn(anotherRole);

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
    }

    @DisplayName("Update role with very long name")
    @Test
    void test_update_role_with_long_name() {
        // Arrange
        Long roleId = 1L;
        String longName = "A".repeat(500); // Very long name
        String normalizedLongName = "a".repeat(500); // Expected normalized name
        
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("ShortName");
        
        Role updatedRole = new Role();
        updatedRole.setName(longName);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName(normalizedLongName)).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert - Should succeed as length validation is handled by database constraints
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(normalizedLongName, updatedRole.getName()); // Expect normalized name
    }

    @DisplayName("Update role preserves ID correctly")
    @Test
    void test_update_role_preserves_id() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("ExistingRole");
        
        Role updatedRole = new Role();
        updatedRole.setId(999L); // Different ID that should be overridden
        updatedRole.setName("UpdatedRole");
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert - Should preserve the path variable ID, not the one in the role object
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(roleId, updatedRole.getId()); // Should be set to path variable ID
        assertNotEquals(999L, updatedRole.getId());
    }

    @DisplayName("Update role with special characters in name")
    @Test
    void test_update_role_with_special_characters() {
        // Arrange
        Long roleId = 1L;
        String specialName = "Admin@#$%^&*()[]{}|;:'\",.<>?/~`";
        String normalizedSpecialName = "admin@#$%^&*()[]{}|;:'\",.<>?/~`"; // Expected normalized name
        
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("RegularName");
        
        Role updatedRole = new Role();
        updatedRole.setName(specialName);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName(normalizedSpecialName)).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(normalizedSpecialName, updatedRole.getName()); // Expect normalized name
    }

    @DisplayName("Update role with whitespace-only name")
    @Test
    void test_update_role_with_whitespace_name() {
        // Arrange
        Long roleId = 1L;
        String whitespaceName = "   \t\n   ";
        String normalizedWhitespaceName = ""; // Expected normalized name (empty string)
        
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("RegularName");
        
        Role updatedRole = new Role();
        updatedRole.setName(whitespaceName);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName(normalizedWhitespaceName)).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(normalizedWhitespaceName, updatedRole.getName()); // Expect normalized name
    }

    @DisplayName("Update role with numeric name")
    @Test
    void test_update_role_with_numeric_name() {
        // Arrange
        Long roleId = 1L;
        String numericName = "12345";
        
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("TextName");
        
        Role updatedRole = new Role();
        updatedRole.setName(numericName);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName(numericName)).thenReturn(null);

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(numericName, updatedRole.getName());
    }

    @DisplayName("Update role description only")
    @Test
    void test_update_role_description_only() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("SameName");
        existingRole.setDescription("Old Description");
        
        Role updatedRole = new Role();
        updatedRole.setName("SameName"); // Same name
        updatedRole.setDescription("New Description"); // Only description changes
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("SameName")).thenReturn(existingRole);

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals("New Description", updatedRole.getDescription());
        assertEquals(roleId, updatedRole.getId());
    }

    @DisplayName("Update role with null description")
    @Test
    void test_update_role_with_null_description() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("RoleName");
        existingRole.setDescription("Old Description");
        
        Role updatedRole = new Role();
        updatedRole.setName("UpdatedName");
        updatedRole.setDescription(null); // Null description
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("updatedname")).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals("updatedname", updatedRole.getName()); // Expect normalized name
        assertEquals(null, updatedRole.getDescription());
    }

    @DisplayName("Update role with disabled flag change")
    @Test
    void test_update_role_disabled_flag() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("RoleName");
        existingRole.setDisabled(false);
        
        Role updatedRole = new Role();
        updatedRole.setName("UpdatedName");
        updatedRole.setDisabled(true); // Change disabled status
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("updatedname")).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals("updatedname", updatedRole.getName()); // Expect normalized name
        assertTrue(updatedRole.getDisabled());
    }

    @DisplayName("Update role with Unicode characters in name")
    @Test
    void test_update_role_with_unicode_name() {
        // Arrange
        Long roleId = 1L;
        String unicodeName = "Rôle Ñame 中文 🚀";
        String normalizedUnicodeName = "rôleñame中文🚀"; // Expected normalized name (lowercase, no spaces)
        
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("RegularName");
        
        Role updatedRole = new Role();
        updatedRole.setName(unicodeName);
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName(normalizedUnicodeName)).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals(normalizedUnicodeName, updatedRole.getName()); // Expect normalized name
    }

    @DisplayName("Update role when existing role has null permissions and users")
    @Test
    void test_update_role_with_null_relationships() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("ExistingRole");
        existingRole.setPermissions(null); // Null relationships
        existingRole.setAppUsers(null);
        
        Role updatedRole = new Role();
        updatedRole.setName("UpdatedRole");
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("updatedrole")).thenReturn(null); // Normalized lookup

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals("updatedrole", updatedRole.getName()); // Expect normalized name
        assertEquals(null, updatedRole.getPermissions());
    }

    @DisplayName("Update role concurrent modification scenario")
    @Test
    void test_update_role_concurrent_modification() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("OriginalName");
        
        Role updatedRole = new Role();
        updatedRole.setName("Updated Name"); // Will normalize to "updatedname"
        
        Role concurrentRole = new Role();
        concurrentRole.setId(2L);
        concurrentRole.setName("updatedname"); // Another role got this normalized name
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("updatedname")).thenReturn(concurrentRole); // Concurrent modification

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
    }

    @DisplayName("Update role with case normalization prevents conflicts")
    @Test
    void test_update_role_case_sensitive_name() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("admin");
        
        Role updatedRole = new Role();
        updatedRole.setName("ADMIN"); // Will normalize to "admin", same as existing
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("admin")).thenReturn(existingRole); // Same role returned

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert - Should succeed because it's the same role after normalization
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals("admin", updatedRole.getName()); // Normalized name
        verify(response, never()).setStatus(Mockito.anyInt());
    }

    // Additional tests for role name normalization
    @DisplayName("Create role normalizes name to lowercase with no spaces")
    @Test
    void test_create_role_normalizes_name() {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role newRole = new Role();
        newRole.setName("Admin Role With Spaces");

        when(roleRepository.getRoleByName("adminrolewithspaces")).thenReturn(null);

        // Act
        rolesApi.create(newRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(newRole);
        assertEquals("adminrolewithspaces", newRole.getName());
        verify(response, never()).setStatus(HttpServletResponse.SC_CONFLICT);
    }

    @DisplayName("Update role normalizes name to lowercase with no spaces")
    @Test
    void test_update_role_normalizes_name() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("originalname");
        
        Role updatedRole = new Role();
        updatedRole.setName("Updated Role Name");
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("updatedrolename")).thenReturn(null);

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals("updatedrolename", updatedRole.getName());
        assertEquals(roleId, updatedRole.getId());
    }

    @DisplayName("Create role with mixed case and spaces normalizes correctly")
    @Test
    void test_create_role_mixed_case_spaces() {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role newRole = new Role();
        newRole.setName("SuPeR    AdMiN   RoLe");

        when(roleRepository.getRoleByName("superadminrole")).thenReturn(null);

        // Act
        rolesApi.create(newRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(newRole);
        assertEquals("superadminrole", newRole.getName());
    }

    @DisplayName("Update role with normalized name that conflicts returns 400")
    @Test
    void test_update_role_normalized_name_conflict() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("originalrole");
        
        Role anotherRole = new Role();
        anotherRole.setId(2L);
        anotherRole.setName("existingrole");
        
        Role updatedRole = new Role();
        updatedRole.setName("Existing Role"); // Will be normalized to "existingrole"
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("existingrole")).thenReturn(anotherRole);

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(response, times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(roleRepository, never()).saveAndFlush(Mockito.any());
    }

    @DisplayName("Create role with normalized name that already exists returns conflict")
    @Test
    void test_create_role_normalized_name_exists() {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role existingRole = new Role();
        existingRole.setName("existingrole");

        Role newRole = new Role();
        newRole.setName("Existing Role"); // Will be normalized to "existingrole"

        when(roleRepository.getRoleByName("existingrole")).thenReturn(existingRole);

        // Act
        rolesApi.create(newRole, response);

        // Assert
        verify(roleRepository, never()).saveAndFlush(newRole);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_CONFLICT);
    }

    @DisplayName("Role name with tabs and newlines are normalized")
    @Test
    void test_normalize_role_name_tabs_newlines() {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role newRole = new Role();
        newRole.setName("Role\t\nWith\r\nWhitespace");

        when(roleRepository.getRoleByName("rolewithwhitespace")).thenReturn(null);

        // Act
        rolesApi.create(newRole, response);

        // Assert
        assertEquals("rolewithwhitespace", newRole.getName());
        verify(roleRepository, times(1)).saveAndFlush(newRole);
    }

    @DisplayName("Role name normalization handles null name")
    @Test
    void test_normalize_null_role_name() {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role newRole = new Role();
        newRole.setName(null);

        when(roleRepository.getRoleByName(null)).thenReturn(null);

        // Act & Assert - This should handle null gracefully
        rolesApi.create(newRole, response);
        
        // The normalization should return null for null input
        assertEquals(null, newRole.getName());
        verify(roleRepository, times(1)).saveAndFlush(newRole);
    }

    @DisplayName("Role name with only spaces becomes empty string")
    @Test
    void test_normalize_spaces_only_role_name() {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role newRole = new Role();
        newRole.setName("   \t  \n  ");

        when(roleRepository.getRoleByName("")).thenReturn(null);

        // Act
        rolesApi.create(newRole, response);

        // Assert
        assertEquals("", newRole.getName());
        verify(roleRepository, times(1)).saveAndFlush(newRole);
    }

    @DisplayName("Update preserves existing normalized name correctly")
    @Test
    void test_update_role_same_normalized_name() {
        // Arrange
        Long roleId = 1L;
        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setName("adminrole");
        
        Role updatedRole = new Role();
        updatedRole.setName("Admin Role"); // Will normalize to "adminrole"
        updatedRole.setDescription("Updated description");
        
        HttpServletResponse response = mock(HttpServletResponse.class);
        
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("adminrole")).thenReturn(existingRole); // Same role

        // Act
        rolesApi.update(roleId, updatedRole, response);

        // Assert
        verify(roleRepository, times(1)).saveAndFlush(updatedRole);
        assertEquals("adminrole", updatedRole.getName());
        assertEquals(roleId, updatedRole.getId());
        verify(response, never()).setStatus(Mockito.anyInt());
    }

}
