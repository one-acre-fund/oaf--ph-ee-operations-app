package org.apache.fineract.api;

import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.permission.PermissionRepository;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RoleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;


import java.util.*;

class RolesApiTest {
    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    RolesApi rolesApi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
        Permission permission1 = new Permission();
        permission1.setId(1L);
        Permission permission2 = new Permission();
        permission2.setId(2L);
        Collection<Permission> permissions = Arrays.asList(permission1, permission2);
        role.setPermissions(permissions);

        Mockito.when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        Collection<Permission> result = rolesApi.retrievePermissions(roleId, response);

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(permission1));
        Assertions.assertTrue(result.contains(permission2));
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
        existingRole.setName("existingRoleName");

        when(roleRepository.getRoleByName("existingRoleName")).thenReturn(existingRole);

        rolesApi.create(existingRole, response);

        verify(roleRepository, never()).saveAndFlush(existingRole);
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

        Mockito.when(roleRepository.existsById(roleId)).thenReturn(true);

        // Act
        rolesApi.delete(roleId, response);

        // Assert
        Mockito.verify(roleRepository, Mockito.times(1)).deleteById(roleId);
        Mockito.verify(response, Mockito.never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Attempt to delete a role with a null ID")
    @Test
    void delete_role_with_null_id() {
        // Arrange
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Long roleId = null;

        // Act
        rolesApi.delete(roleId, response);

        // Assert
        Mockito.verify(roleRepository, Mockito.never()).deleteById(Mockito.any());
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Assign permissions to a role successfully")
    @Test
    void test_assign_permissions_successfully() {
        // Arrange
        Long roleId = 1L;
        AssignmentAction action = AssignmentAction.ASSIGN;
        EntityAssignments assignments = new EntityAssignments();
        // list
        List<Long> entityIds = new ArrayList<>();
        entityIds.add(2L);
        entityIds.add(3L);
        assignments.setEntityIds(entityIds);

        Role existingRole = new Role();
        existingRole.setId(roleId);
        existingRole.setPermissions(new ArrayList<>());

        Permission permission1 = new Permission();
        permission1.setId(2L);
        Permission permission2 = new Permission();
        permission2.setId(3L);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(permission1));
        when(permissionRepository.findById(3L)).thenReturn(Optional.of(permission2));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        rolesApi.permissionAssignment(roleId, action, assignments, response);

        // Assert
        assertEquals(2, existingRole.getPermissions().size());
        verify(roleRepository, times(1)).saveAndFlush(existingRole);
    }

}