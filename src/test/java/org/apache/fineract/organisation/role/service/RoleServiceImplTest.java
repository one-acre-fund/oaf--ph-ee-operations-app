package org.apache.fineract.organisation.role.service;

import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RoleRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.permission.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role testRole;
    private Role existingRole;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testRole = new Role();
        testRole.setName("Test Role");
        testRole.setDescription("Test Description");
        testRole.setDisabled(false);

        existingRole = new Role();
        existingRole.setId(1L);
        existingRole.setName("existingrole");
        existingRole.setDescription("Existing role");
        existingRole.setDisabled(false);
        existingRole.setAppUsers(new ArrayList<>());
        existingRole.setPermissions(new ArrayList<>());
    }

    @Test
    @DisplayName("Create role with unique name should succeed")
    void createRole_withUniqueName_shouldSucceed() {
        // Given
        String normalizedName = "testrole";
        when(roleRepository.getRoleByName(normalizedName)).thenReturn(null);

        // When
        roleService.createRole(testRole, response);

        // Then
        verify(roleRepository).getRoleByName(normalizedName);
        verify(roleRepository).saveAndFlush(testRole);
        verify(response, never()).setStatus(anyInt());
        assertEquals(normalizedName, testRole.getName());
        assertNull(testRole.getId()); // ID should be set to null before saving
    }

    @Test
    @DisplayName("Create role with existing name should return conflict")
    void createRole_withExistingName_shouldReturnConflict() {
        // Given
        String normalizedName = "testrole";
        Role existingRole = new Role();
        existingRole.setName(normalizedName);
        when(roleRepository.getRoleByName(normalizedName)).thenReturn(existingRole);

        // When
        roleService.createRole(testRole, response);

        // Then
        verify(roleRepository).getRoleByName(normalizedName);
        verify(roleRepository, never()).saveAndFlush(any());
        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
        assertEquals(normalizedName, testRole.getName());
    }

    @Test
    @DisplayName("Create role normalizes name correctly")
    void createRole_normalizesNameCorrectly() {
        // Given
        testRole.setName("Test Role With Spaces");
        String expectedNormalized = "testrolewithspaces";
        when(roleRepository.getRoleByName(expectedNormalized)).thenReturn(null);

        // When
        roleService.createRole(testRole, response);

        // Then
        verify(roleRepository).getRoleByName(expectedNormalized);
        assertEquals(expectedNormalized, testRole.getName());
    }

    @Test
    @DisplayName("Create role with null name should handle gracefully")
    void createRole_withNullName_shouldHandleGracefully() {
        // Given
        testRole.setName(null);
        when(roleRepository.getRoleByName(null)).thenReturn(null);

        // When
        roleService.createRole(testRole, response);

        // Then
        verify(roleRepository).getRoleByName(null);
        verify(roleRepository).saveAndFlush(testRole);
        assertNull(testRole.getName());
    }

    @Test
    @DisplayName("Update role with valid ID should succeed")
    void updateRole_withValidId_shouldSucceed() {
        // Given
        Long roleId = 1L;
        Role updateRole = new Role();
        updateRole.setName("Updated Role");
        updateRole.setDescription("Updated description");
        updateRole.setDisabled(true);

        String normalizedName = "updatedrole";
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName(normalizedName)).thenReturn(null);

        // When
        roleService.updateRole(roleId, updateRole, response);

        // Then
        verify(roleRepository).findById(roleId);
        verify(roleRepository).getRoleByName(normalizedName);
        verify(roleRepository).saveAndFlush(updateRole);
        verify(response, never()).setStatus(anyInt());

        assertEquals(roleId, updateRole.getId());
        assertEquals(normalizedName, updateRole.getName());
        assertEquals(existingRole.getAppusers(), updateRole.getAppusers());
        assertEquals(existingRole.getPermissions(), updateRole.getPermissions());
    }

    @Test
    @DisplayName("Update role with non-existent ID should return not found")
    void updateRole_withNonExistentId_shouldReturnNotFound() {
        // Given
        Long roleId = 999L;
        Role updateRole = new Role();
        updateRole.setName("Updated Role");

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // When
        roleService.updateRole(roleId, updateRole, response);

        // Then
        verify(roleRepository).findById(roleId);
        verify(roleRepository, never()).saveAndFlush(any());
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("Update role with same name as existing should succeed")
    void updateRole_withSameNameAsExisting_shouldSucceed() {
        // Given
        Long roleId = 1L;
        Role updateRole = new Role();
        updateRole.setName("existingrole"); // Same as existing role's name

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));

        // When
        roleService.updateRole(roleId, updateRole, response);

        // Then
        verify(roleRepository).findById(roleId);
        verify(roleRepository, never()).getRoleByName(anyString()); // Should not check for duplicate name
        verify(roleRepository).saveAndFlush(updateRole);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Update role with duplicate name should return bad request")
    void updateRole_withDuplicateName_shouldReturnBadRequest() {
        // Given
        Long roleId = 1L;
        Role updateRole = new Role();
        updateRole.setName("Another Role");
        String normalizedName = "anotherrole";

        Role duplicateRole = new Role();
        duplicateRole.setId(2L);
        duplicateRole.setName(normalizedName);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName(normalizedName)).thenReturn(duplicateRole);

        // When
        roleService.updateRole(roleId, updateRole, response);

        // Then
        verify(roleRepository).findById(roleId);
        verify(roleRepository).getRoleByName(normalizedName);
        verify(roleRepository, never()).saveAndFlush(any());
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("Update role preserves existing relationships")
    void updateRole_preservesExistingRelationships() {
        // Given
        Long roleId = 1L;
        Role updateRole = new Role();
        updateRole.setName("Updated Role");

        // Add some test data to existing role
        Collection<AppUser> users = new ArrayList<>();
        Collection<Permission> permissions = new ArrayList<>();
        existingRole.setAppUsers(users);
        existingRole.setPermissions(permissions);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));
        when(roleRepository.getRoleByName("updatedrole")).thenReturn(null);

        // When
        roleService.updateRole(roleId, updateRole, response);

        // Then
        assertEquals(users, updateRole.getAppusers());
        assertEquals(permissions, updateRole.getPermissions());
    }

    @Test
    @DisplayName("Role name normalization handles various cases")
    void normalizeRoleName_handlesVariousCases() {
        // Test various normalization scenarios
        testNormalization("Simple Role", "simplerole");
        testNormalization("UPPERCASE ROLE", "uppercaserole");
        testNormalization("Role   With   Multiple   Spaces", "rolewithmultiplespaces");
        testNormalization("Role\tWith\tTabs", "rolewithtabs");
        testNormalization("Role\nWith\nNewlines", "rolewithnewlines");
        testNormalization("Role\r\nWith\r\nCarriageReturns", "rolewithcarriagereturns");
        testNormalization("   Leading and Trailing   ", "leadingandtrailing");
        testNormalization("", "");
    }

    @Test
    @DisplayName("Role name normalization handles null input")
    void normalizeRoleName_handlesNullInput() {
        // Given
        testRole.setName(null);
        when(roleRepository.getRoleByName(null)).thenReturn(null);

        // When
        roleService.createRole(testRole, response);

        // Then
        assertNull(testRole.getName());
    }

    @Test
    @DisplayName("Update role with same normalized name but different case should succeed")
    void updateRole_withSameNormalizedNameDifferentCase_shouldSucceed() {
        // Given
        Long roleId = 1L;
        existingRole.setName("existingrole");
        
        Role updateRole = new Role();
        updateRole.setName("EXISTING ROLE"); // Different case, same normalized

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(existingRole));

        // When
        roleService.updateRole(roleId, updateRole, response);

        // Then
        verify(roleRepository).findById(roleId);
        verify(roleRepository, never()).getRoleByName(anyString()); // Should not check for duplicate
        verify(roleRepository).saveAndFlush(updateRole);
        verify(response, never()).setStatus(anyInt());
        assertEquals("existingrole", updateRole.getName());
    }

    private void testNormalization(String input, String expected) {
        Role role = new Role();
        role.setName(input);
        when(roleRepository.getRoleByName(expected)).thenReturn(null);

        roleService.createRole(role, response);

        assertEquals(expected, role.getName());
        reset(roleRepository, response);
    }
}



