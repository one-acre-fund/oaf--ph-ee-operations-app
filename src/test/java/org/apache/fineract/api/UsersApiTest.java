package org.apache.fineract.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RoleRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsersApiTest {
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppUserRepository appuserRepository;

    @Mock
    private RoleRepository roleRepository;
    @InjectMocks
    private UsersApi usersApi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    @DisplayName("retrieve all users successfully when users exist in the repository")
    void test_retrieve_all_users_successfully() {
        // Arrange
        AppUser user1 = new AppUser();
        user1.setUsername("user1");
        AppUser user2 = new AppUser();
        user2.setUsername("user2");
        List<AppUser> users = Arrays.asList(user1, user2);

        when(appuserRepository.findAll()).thenReturn(users);
        // Act
        List<AppUser> result = usersApi.retrieveAll();

        // Assert
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(user1));
        Assertions.assertTrue(result.contains(user2));
    }

    @DisplayName("handle the scenario when the repository is empty")
    @Test
    void test_handle_empty_repository() {
        // Arrange
        when(appuserRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<AppUser> result = usersApi.retrieveAll();

        // Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @DisplayName("Retrieve an existing user by ID")
    @Test
    void test_retrieve_existing_user_by_id() {
        // Arrange
        Long userId = 1L;
        AppUser expectedUser = new AppUser();
        expectedUser.setId(userId);
        expectedUser.setUsername("testuser");

        when(appuserRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
        HttpServletResponse response = mock(HttpServletResponse.class);
        // Act
        AppUser actualUser = usersApi.retrieveOne(userId, response);

        // Assert
        Assertions.assertNotNull(actualUser);
        Assertions.assertEquals(expectedUser.getUsername(), actualUser.getUsername());
    }

    @DisplayName("Retrieve roles for an existing user")
    @Test
    void test_retrieve_roles_existing_user() {
        // Arrange
        Long userId = 1L;
        AppUser user = new AppUser();
        Role role1 = new Role();
        role1.setName("Admin");
        Role role2 = new Role();
        role2.setName("User");
        user.setRoles(Arrays.asList(role1, role2));

        when(appuserRepository.findById(userId)).thenReturn(Optional.of(user));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        Collection<Role> roles = usersApi.retrieveRoles(userId, response);

        // Assert
        assertNotNull(roles);
        assertEquals(2, roles.size());
    }

    @DisplayName("Creating a new user with valid data should save the user and return the user as JSON")
    @Test
    void create_new_user_success() throws IOException {
        // Arrange
        AppUser appUser = new AppUser();
        appUser.setUsername("newuser");
        appUser.setPassword("password123");
        appUser.setEmail("newuser@example.com");
        appUser.setFirstname("New");
        appUser.setLastname("User");

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        StringWriter responseWriter = new StringWriter();
        when(mockResponse.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(appuserRepository.findAppUserByName("newuser")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");


        // Act
        usersApi.create(appUser, mockResponse);

        // Assert
        verify(appuserRepository).saveAndFlush(appUser);
        assertEquals("encodedPassword123", appUser.getPassword());
        String expectedJson = new ObjectMapper().writeValueAsString(appUser);
        assertEquals(expectedJson, responseWriter.toString().trim());
    }

    @DisplayName("Creating a user with an existing username should return a conflict status")
    @Test
    void create_user_conflict() throws IOException {
        // Arrange
        AppUser appUser = new AppUser();
        appUser.setUsername("existinguser");
        appUser.setPassword("password123");
        appUser.setEmail("existinguser@example.com");
        appUser.setFirstname("Existing");
        appUser.setLastname("User");

        AppUser existingAppUser = new AppUser();
        existingAppUser.setUsername("existinguser");

        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        when(appuserRepository.findAppUserByName("existinguser")).thenReturn(existingAppUser);

        // Act
        usersApi.create(appUser, mockResponse);

        // Assert
        verify(mockResponse).setStatus(HttpServletResponse.SC_CONFLICT);
    }

    @DisplayName("Update an existing user with valid data")
    @Test
    void update_existing_user_with_valid_data() {
        // Arrange
        Long userId = 1L;
        AppUser existingUser = new AppUser();
        existingUser.setId(userId);
        existingUser.setPassword("oldPassword");
        existingUser.setRoles(new ArrayList<>());

        AppUser updatedUser = new AppUser();
        updatedUser.setPassword("newPassword");

        HttpServletResponse response = mock(HttpServletResponse.class);
        when(appuserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        // Act
        usersApi.update(userId, updatedUser, response);

        // Assert
        assertEquals(userId, updatedUser.getId());
        assertEquals("encodedNewPassword", updatedUser.getPassword());
        assertEquals(existingUser.getRoles(), updatedUser.getRoles());
        verify(appuserRepository).saveAndFlush(updatedUser);
    }

    @DisplayName("Update a user that does not exist")
    @Test
    void update_nonexistent_user() {
        // Arrange
        Long userId = 1L;
        AppUser updatedUser = new AppUser();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(appuserRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        usersApi.update(userId, updatedUser, response);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(appuserRepository, never()).saveAndFlush(any(AppUser.class));
    }

    @DisplayName("Deactivate an enabled user successfully")
    @Test
    void test_deactivate_enabled_user_successfully() {
        Long userId = 1L;
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEnabled(true);

        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);

        Mockito.when(appuserRepository.findById(userId)).thenReturn(Optional.of(user));
        Mockito.when(appuserRepository.saveAndFlush(user)).thenReturn(user);


        AppUser result = usersApi.deactivate(userId, mockResponse);

        assertNotNull(result);
        assertFalse(result.isEnabled());
        Mockito.verify(appuserRepository).saveAndFlush(user);
    }

    @DisplayName("Attempt to deactivate a user that does not exist")
    @Test
    void test_deactivate_nonexistent_user() {
        Long userId = 1L;

        AppUserRepository mockRepository = Mockito.mock(AppUserRepository.class);
        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);

        Mockito.when(mockRepository.findById(userId)).thenReturn(Optional.empty());


        AppUser result = usersApi.deactivate(userId, mockResponse);

        assertNull(result);
        Mockito.verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Successfully delete an existing user by ID")
    @Test
    void test_delete_existing_user() {
        // Arrange
        Long userId = 1L;
        AppUser user = new AppUser();
        user.setId(userId);

        when(appuserRepository.findById(userId)).thenReturn(Optional.of(user));


        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        usersApi.delete(userId, response);

        // Assert
        verify(appuserRepository, times(1)).deleteById(userId);
        verify(response, never()).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Attempt to delete a user with a non-existent ID")
    @Test
    void test_delete_non_existent_user() {
        // Arrange
        Long userId = 999L;

        when(appuserRepository.findById(userId)).thenReturn(Optional.empty());
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        usersApi.delete(userId, response);

        // Assert
        verify(appuserRepository, never()).deleteById(userId);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    @DisplayName("Assigning currencies to an existing user updates the user's currencies list")
    @Test
    void test_assigning_currencies_to_existing_user() {
        // Arrange
        Long userId = 1L;
        List<String> currencies = Arrays.asList("USD", "EUR");
        AppUser user = new AppUser();
        user.setId(userId);
        user.setCurrenciesList(new ArrayList<>());

        Mockito.when(appuserRepository.findById(userId)).thenReturn(Optional.of(user));

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        usersApi.userCurrenciesAssignment(userId, currencies, response);

        // Assert
        Mockito.verify(appuserRepository).saveAndFlush(user);
        assertEquals(currencies, user.getCurrenciesList());
    }

    @DisplayName("Attempting to assign currencies to a non-existent user results in a 404 Not Found response")
    @Test
    void test_assigning_currencies_to_non_existent_user() {
        // Arrange
        Long userId = 1L;
        List<String> currencies = Arrays.asList("USD", "EUR");

        Mockito.when(appuserRepository.findById(userId)).thenReturn(Optional.empty());

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        usersApi.userCurrenciesAssignment(userId, currencies, response);

        // Assert
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Assign payeePartyIds to an existing user")
    @Test
    void test_assign_payee_party_ids_to_existing_user() {
        // Arrange
        Long userId = 1L;
        List<String> payeePartyIds = Arrays.asList("ID1", "ID2");
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPayeePartyIdsList(new ArrayList<>());

        when(appuserRepository.findById(userId)).thenReturn(Optional.of(user));

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        usersApi.userPayeePartyIdsAssignment(userId, payeePartyIds, response);

        // Assert
        verify(appuserRepository).saveAndFlush(user);
        assertEquals(payeePartyIds, user.getPayeePartyIdsList());
    }

    @DisplayName("User ID does not exist in the repository")
    @Test
    void test_user_id_does_not_exist_in_repository() {
        // Arrange
        Long userId = 1L;
        List<String> payeePartyIds = Arrays.asList("ID1", "ID2");

        when(appuserRepository.findById(userId)).thenReturn(Optional.empty());


        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        usersApi.userPayeePartyIdsAssignment(userId, payeePartyIds, response);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Successfully assigns payeePartyIdTypes to an existing user")
    @Test
    void test_assign_payee_party_id_types_to_existing_user() {
        // Arrange
        Long userId = 1L;
        List<String> payeePartyIdTypes = Arrays.asList("TYPE1", "TYPE2");
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPayeePartyIdTypesList(new ArrayList<>());

        Mockito.when(appuserRepository.findById(userId)).thenReturn(Optional.of(user));


        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        usersApi.userPayeePartyIdTypesAssignment(userId, payeePartyIdTypes, response);

        // Assert
        Mockito.verify(appuserRepository).saveAndFlush(user);
        assertEquals(payeePartyIdTypes, user.getPayeePartyIdTypesList());
    }

    @DisplayName("User ID does not exist in the repository")
    @Test
    void test_user_id_not_found() {
        // Arrange
        Long userId = 1L;
        List<String> payeePartyIdTypes = Arrays.asList("TYPE1", "TYPE2");

        Mockito.when(appuserRepository.findById(userId)).thenReturn(Optional.empty());


        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // Act
        usersApi.userPayeePartyIdTypesAssignment(userId, payeePartyIdTypes, response);

        // Assert
        Mockito.verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Assign roles to an existing user successfully")
    @Test
    void assign_roles_to_existing_user_successfully() {
        // Arrange
        Long userId = 1L;
        AssignmentAction action = AssignmentAction.ASSIGN;
        EntityAssignments assignments = new EntityAssignments();
        List<Long> entityList = Arrays.asList(2L, 3L);

        assignments.setEntityIds(entityList);

        AppUser existingUser = new AppUser();
        existingUser.setId(userId);
        existingUser.setRoles(new ArrayList<>());

        Role role1 = new Role();
        role1.setId(2L);
        Role role2 = new Role();
        role2.setId(3L);

        HttpServletResponse response = mock(HttpServletResponse.class);


        when(appuserRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(role1));
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role2));

        // Act
        usersApi.userAssignment(userId, action, assignments, response);

        // Assert
        assertEquals(2, existingUser.getRoles().size());
        assertTrue(existingUser.getRoles().contains(role1));
        assertTrue(existingUser.getRoles().contains(role2));
    }

    @DisplayName("Handle non-existing user ID gracefully")
    @Test
    void handle_non_existing_user_id_gracefully() {
        // Arrange
        Long userId = 999L;
        AssignmentAction action = AssignmentAction.ASSIGN;
        EntityAssignments assignments = new EntityAssignments();
        List<Long> entityList = Arrays.asList(2L, 3L);
        assignments.setEntityIds(entityList);

        when(appuserRepository.findById(userId)).thenReturn(Optional.empty());

        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        usersApi.userAssignment(userId, action, assignments, response);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @DisplayName("Activating a user successfully when the user exists and is not enabled")
    @Test
    void test_activate_user_successfully() {
        // Arrange
        Long userId = 1L;
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEnabled(false);

        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);

        Mockito.when(appuserRepository.findById(userId)).thenReturn(Optional.of(user));
        Mockito.when(appuserRepository.saveAndFlush(user)).thenReturn(user);


        // Act
        AppUser result = usersApi.activate(userId, mockResponse);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEnabled());
        Mockito.verify(appuserRepository).saveAndFlush(user);
    }

    @DisplayName("Handling the scenario where the user does not exist")
    @Test
    void test_activate_user_not_found() {
        // Arrange
        Long userId = 1L;

        HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);

        Mockito.when(appuserRepository.findById(userId)).thenReturn(Optional.empty());


        // Act
        AppUser result = usersApi.activate(userId, mockResponse);

        // Assert
        assertNull(result);
        Mockito.verify(mockResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

}