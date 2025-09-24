package org.apache.fineract.api;

import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.permission.PermissionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionsApiTest {

    @Mock
    private PermissionRepository permissionRepository;
    @InjectMocks
    private PermissionsApi permissionsApi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Retrieve all permissions successfully")
    void test_retrieve_all_permissions_successfully() {
        List<Permission> mockPermissions = Arrays.asList(new Permission(), new Permission());
        when(permissionRepository.findAll()).thenReturn(mockPermissions);

        List<Permission> result = permissionsApi.retrieveAll();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
    }
    @Test
    @DisplayName("Permission repository returns null")
    void test_permission_repository_returns_null() {
        when(permissionRepository.findAll()).thenReturn(null);

        List<Permission> result = permissionsApi.retrieveAll();

        Assertions.assertNull(result);
    }

    @Test
    @DisplayName("Retrieve permission by valid id")
    void test_retrieve_permission_by_valid_id() {
        // Arrange
        Long validPermissionId = 1L;
        Permission expectedPermission = new Permission();
        expectedPermission.setId(validPermissionId);
        expectedPermission.setCode("PERMISSION_CODE");

        when(permissionRepository.findById(validPermissionId)).thenReturn(Optional.of(expectedPermission));


        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        Permission actualPermission = permissionsApi.retrieveOne(validPermissionId, response);

        // Assert
        Assertions.assertNotNull(actualPermission);
        Assertions.assertEquals(expectedPermission, actualPermission);
    }


    @Test
    @DisplayName("Creating a new permission when no existing permission with the same code exists")
    void test_create_new_permission() {
        Permission newPermission = new Permission();
        newPermission.setCode("NEW_CODE");

        when(permissionRepository.findOneByCode("NEW_CODE")).thenReturn(null);

        HttpServletResponse response = mock(HttpServletResponse.class);

        permissionsApi.create(newPermission, response);

        verify(permissionRepository).saveAndFlush(newPermission);
        verify(response, never()).setStatus(HttpServletResponse.SC_CONFLICT);
    }


    @Test
    @DisplayName("Attempting to create a permission with a code that already exists")
    void test_create_existing_permission() {
        Permission existingPermission = new Permission();
        existingPermission.setCode("EXISTING_CODE");

        when(permissionRepository.findOneByCode("EXISTING_CODE")).thenReturn(existingPermission);

        HttpServletResponse response = mock(HttpServletResponse.class);

        permissionsApi.create(existingPermission, response);

        verify(permissionRepository, never()).saveAndFlush(existingPermission);
        verify(response).setStatus(HttpServletResponse.SC_CONFLICT);
    }

    @Test
    @DisplayName("Update existing permission with valid data")
    void test_update_existing_permission_with_valid_data() {

        Long permissionId = 1L;
        Permission existingPermission = new Permission();
        existingPermission.setId(permissionId);
        existingPermission.setCode("EXISTING_CODE");
        existingPermission.setRoles(new ArrayList<>());

        Permission updatedPermission = new Permission();
        updatedPermission.setCode("UPDATED_CODE");

        Mockito.when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(existingPermission));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        permissionsApi.update(permissionId, updatedPermission, response);

        Mockito.verify(permissionRepository).saveAndFlush(updatedPermission);
        assertEquals(permissionId, updatedPermission.getId());
        assertEquals(existingPermission.getRoles(), updatedPermission.getRoles());
    }
    @Test
    @DisplayName("Delete permission when permissionId exists")
    void delete_permission_when_permissionId_exists() {
        // Arrange
        Long permissionId = 1L;
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(permissionRepository.existsById(permissionId)).thenReturn(true);

        // Act
        permissionsApi.delete(permissionId, response);

        // Assert
        Mockito.verify(permissionRepository).deleteById(permissionId);
        Mockito.verify(response, Mockito.never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("Attempt to delete a permission when the permissionId does not exist")
    void delete_permission_when_permissionId_does_not_exist() {
        // Arrange
        Long permissionId = 1L;
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(permissionRepository.existsById(permissionId)).thenReturn(false);

        // Act
        permissionsApi.delete(permissionId, response);

        // Assert
        Mockito.verify(permissionRepository, Mockito.never()).deleteById(permissionId);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

}