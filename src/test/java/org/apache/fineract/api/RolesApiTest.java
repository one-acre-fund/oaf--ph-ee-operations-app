package org.apache.fineract.api;

import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.permission.PermissionData;
import org.apache.fineract.organisation.permission.PermissionRepository;
import org.apache.fineract.organisation.role.PermissionsCommand;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RolePermissionsData;
import org.apache.fineract.organisation.role.RoleRepository;
import org.apache.fineract.organisation.role.service.RoleService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class RolesApiTest {
    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleService roleService;

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
        Assertions.assertThrows(RuntimeException.class, () -> rolesApi.retrievePermissions(roleId, response));

        Mockito.verify(roleRepository, Mockito.never()).findById(Mockito.any());
    }

    // API Tests - These focus on proper delegation to service layer
    @Test
    void test_create_role_delegates_to_service() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Role newRole = new Role();
        newRole.setName("uniqueRoleName");

        rolesApi.create(newRole, response);

        verify(roleService, times(1)).createRole(newRole, response);
    }

    @Test
    void test_update_role_delegates_to_service() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Role updatedRole = new Role();
        updatedRole.setName("Admin Updated");
        updatedRole.setDescription("Updated Administrator role");
        updatedRole.setDisabled(false);

        rolesApi.update(1L, updatedRole, response);

        verify(roleService, times(1)).updateRole(1L, updatedRole, response);
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
        assertTrue(role.getPermissions().isEmpty());
        assertTrue(role.getAppusers().isEmpty());
        Mockito.verify(roleRepository, Mockito.times(1)).saveAndFlush(role);
        Mockito.verify(roleRepository, Mockito.times(1)).deleteById(roleId);
    }

    @DisplayName("Update role status - disable existing role")
    @Test
    void updateRoleStatus_disableExistingRole() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(false);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "disable", response);

        // Assert
        assertTrue(role.getDisabled());
        Mockito.verify(roleRepository, Mockito.times(1)).saveAndFlush(role);
        Mockito.verify(response, Mockito.never()).setStatus(Mockito.anyInt());
    }

    @DisplayName("Update role status - enable existing role")
    @Test
    void updateRoleStatus_enableExistingRole() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setDisabled(true);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "enable", response);

        // Assert
        assertFalse(role.getDisabled());
        Mockito.verify(roleRepository, Mockito.times(1)).saveAndFlush(role);
        Mockito.verify(response, Mockito.never()).setStatus(Mockito.anyInt());
    }

    @DisplayName("Update role status - invalid command returns bad request")
    @Test
    void updateRoleStatus_invalidCommand_returnsBadRequest() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "invalidCommand", response);

        // Assert
        Mockito.verify(roleRepository, Mockito.never()).saveAndFlush(Mockito.any());
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @DisplayName("Update role status - role not found returns 404")
    @Test
    void updateRoleStatus_roleNotFound_returns404() {
        // Arrange
        Long roleId = 999L;

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.updateRoleStatus(roleId, "disable", response);

        // Assert
        Mockito.verify(roleRepository, Mockito.never()).saveAndFlush(Mockito.any());
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Permission assignment - add permissions to role")
    @Test
    void permissionAssignment_addPermissions() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setPermissions(new ArrayList<>());

        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");
        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        List<Permission> allPermissions = Arrays.asList(permission1, permission2);

        Map<String, Boolean> commandPermissions = new HashMap<>();
        commandPermissions.put("READ_USER", true);
        commandPermissions.put("CREATE_USER", true);

        PermissionsCommand permissionsCommand = new PermissionsCommand();
        permissionsCommand.setPermissions(commandPermissions);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        Mockito.when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, permissionsCommand, response);

        // Assert
        assertEquals(2, role.getPermissions().size());
        assertTrue(role.getPermissions().contains(permission1));
        assertTrue(role.getPermissions().contains(permission2));
        Mockito.verify(roleRepository, Mockito.times(1)).saveAndFlush(role);
    }

    @DisplayName("Permission assignment - remove permissions from role")
    @Test
    void permissionAssignment_removePermissions() {
        // Arrange
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);

        Permission permission1 = new Permission();
        permission1.setCode("READ_USER");
        Permission permission2 = new Permission();
        permission2.setCode("CREATE_USER");

        role.setPermissions(new ArrayList<>(Arrays.asList(permission1, permission2)));

        List<Permission> allPermissions = Arrays.asList(permission1, permission2);

        Map<String, Boolean> commandPermissions = new HashMap<>();
        commandPermissions.put("READ_USER", false);
        commandPermissions.put("CREATE_USER", false);

        PermissionsCommand permissionsCommand = new PermissionsCommand();
        permissionsCommand.setPermissions(commandPermissions);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        Mockito.when(permissionRepository.findAll()).thenReturn(allPermissions);

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, permissionsCommand, response);

        // Assert
        assertEquals(0, role.getPermissions().size());
        Mockito.verify(roleRepository, Mockito.times(1)).saveAndFlush(role);
    }

    @DisplayName("Permission assignment - role not found returns 404")
    @Test
    void permissionAssignment_roleNotFound_returns404() {
        // Arrange
        Long roleId = 999L;
        PermissionsCommand permissionsCommand = new PermissionsCommand();

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, permissionsCommand, response);

        // Assert
        Mockito.verify(roleRepository, Mockito.never()).saveAndFlush(Mockito.any());
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

}
