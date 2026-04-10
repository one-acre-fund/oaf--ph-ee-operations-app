package org.apache.fineract.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.exception.NoAuthorizationException;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RoleRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.apache.fineract.organisation.user.AppUserDto;
import org.apache.fineract.users.service.UserService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsersApiTest {
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppUserRepository appuserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UsersApi usersApi;
    @Mock
    AppUser connectedUser;

    private MockedStatic<ThreadLocalContextUtil> mockedThreadLocalContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockedThreadLocalContext = mockStatic(ThreadLocalContextUtil.class);
        mockedThreadLocalContext.when(ThreadLocalContextUtil::getCurrentUser).thenReturn(connectedUser);
    }

    @AfterEach
    void tearDown() {
        if (mockedThreadLocalContext != null) {
            mockedThreadLocalContext.close();
        }
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

        when(appuserRepository.findAll(any(Specification.class))).thenReturn(users);
        // Act
        List<AppUser> result = usersApi.retrieveAll(null, null);

        // Assert
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(user1));
        Assertions.assertTrue(result.contains(user2));
    }

    @DisplayName("handle the scenario when the repository is empty")
    @Test
    void test_handle_empty_repository() {
        // Arrange
        when(appuserRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        // Act
        List<AppUser> result = usersApi.retrieveAll(null, null);

        // Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @DisplayName("retrieveAll with role filter returns only users with that role")
    @Test
    void test_retrieve_all_filtered_by_role() {
        // Arrange
        AppUser admin1 = new AppUser();
        admin1.setUsername("admin1");
        AppUser admin2 = new AppUser();
        admin2.setUsername("admin2");
        List<AppUser> admins = Arrays.asList(admin1, admin2);

        when(appuserRepository.findAll(any(Specification.class))).thenReturn(admins);

        // Act
        List<AppUser> result = usersApi.retrieveAll("Admin", null);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(admin1));
        assertTrue(result.contains(admin2));
        verify(appuserRepository).findAll(any(Specification.class));
    }

    @DisplayName("retrieveAll with enabled=true returns only enabled users")
    @Test
    void test_retrieve_all_filtered_by_enabled_true() {
        // Arrange
        AppUser enabledUser = new AppUser();
        enabledUser.setUsername("activeUser");
        enabledUser.setEnabled(true);
        List<AppUser> enabledUsers = Collections.singletonList(enabledUser);

        when(appuserRepository.findAll(any(Specification.class))).thenReturn(enabledUsers);

        // Act
        List<AppUser> result = usersApi.retrieveAll(null, true);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEnabled());
        verify(appuserRepository).findAll(any(Specification.class));
    }

    @DisplayName("retrieveAll with enabled=false returns only disabled users")
    @Test
    void test_retrieve_all_filtered_by_enabled_false() {
        // Arrange
        AppUser disabledUser = new AppUser();
        disabledUser.setUsername("inactiveUser");
        disabledUser.setEnabled(false);
        List<AppUser> disabledUsers = Collections.singletonList(disabledUser);

        when(appuserRepository.findAll(any(Specification.class))).thenReturn(disabledUsers);

        // Act
        List<AppUser> result = usersApi.retrieveAll(null, false);

        // Assert
        assertEquals(1, result.size());
        assertFalse(result.get(0).isEnabled());
        verify(appuserRepository).findAll(any(Specification.class));
    }

    @DisplayName("retrieveAll with both role and enabled filters returns matching users")
    @Test
    void test_retrieve_all_filtered_by_role_and_enabled() {
        // Arrange
        AppUser user = new AppUser();
        user.setUsername("activeAdmin");
        user.setEnabled(true);
        List<AppUser> filtered = Collections.singletonList(user);

        when(appuserRepository.findAll(any(Specification.class))).thenReturn(filtered);

        // Act
        List<AppUser> result = usersApi.retrieveAll("Admin", true);

        // Assert
        assertEquals(1, result.size());
        assertEquals("activeAdmin", result.get(0).getUsername());
        verify(appuserRepository).findAll(any(Specification.class));
    }

    @DisplayName("retrieveAll with empty role string falls back to no role filter")
    @Test
    void test_retrieve_all_empty_role_string_ignored() {
        // Arrange
        AppUser user = new AppUser();
        user.setUsername("someUser");
        List<AppUser> allUsers = Collections.singletonList(user);

        when(appuserRepository.findAll(any(Specification.class))).thenReturn(allUsers);

        // Act
        List<AppUser> result = usersApi.retrieveAll("", null);

        // Assert
        assertEquals(1, result.size());
        verify(appuserRepository).findAll(any(Specification.class));
    }

    @DisplayName("Retrieve an existing user by ID")
    @Test
    void test_retrieve_existing_user_by_id() {
        // Arrange
        Long userId = 1L;
        AppUser expectedUser = new AppUser();
        expectedUser.setId(userId);
        expectedUser.setUsername("testuser");

        when(userService.retrieveUserById(anyLong(), any(HttpServletResponse.class))).thenReturn(new AppUserDto(expectedUser, Set.of(), Set.of()));
        HttpServletResponse response = mock(HttpServletResponse.class);
        // Act
        AppUserDto actualUser = usersApi.retrieveOne(userId, response);

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

    @DisplayName("retrieveAllUsersWithRolesExcel returns Excel with correct headers and user data")
    @Test
    void test_retrieve_all_users_with_roles_excel_success() throws IOException {
        // Arrange
        Role adminRole = new Role();
        adminRole.setName("Admin");
        Role userRole = new Role();
        userRole.setName("User");

        AppUser user1 = new AppUser();
        user1.setId(1L);
        user1.setUsername("alice");
        user1.setRoles(Arrays.asList(adminRole, userRole));

        AppUser user2 = new AppUser();
        user2.setId(2L);
        user2.setUsername("bob");
        user2.setRoles(Collections.emptyList());

        when(appuserRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        usersApi.retrieveAllUsersWithRolesExcel(response);

        // Assert
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        assertEquals("attachment; filename=users-with-roles.xlsx", response.getHeader("Content-Disposition"));

        byte[] excelBytes = response.getContentAsByteArray();
        assertTrue(excelBytes.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Users");
            assertNotNull(sheet);

            // Header row
            Row headerRow = sheet.getRow(0);
            assertEquals("ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Username", headerRow.getCell(1).getStringCellValue());
            assertEquals("Roles", headerRow.getCell(2).getStringCellValue());

            // First data row
            Row row1 = sheet.getRow(1);
            assertEquals(1.0, row1.getCell(0).getNumericCellValue());
            assertEquals("alice", row1.getCell(1).getStringCellValue());
            assertEquals("Admin, User", row1.getCell(2).getStringCellValue());

            // Second data row
            Row row2 = sheet.getRow(2);
            assertEquals(2.0, row2.getCell(0).getNumericCellValue());
            assertEquals("bob", row2.getCell(1).getStringCellValue());
            assertEquals("", row2.getCell(2).getStringCellValue());
        }
    }

    @DisplayName("retrieveAllUsersWithRolesExcel returns Excel with only header row when no users exist")
    @Test
    void test_retrieve_all_users_with_roles_excel_empty_users() throws IOException {
        // Arrange
        when(appuserRepository.findAll()).thenReturn(Collections.emptyList());
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        usersApi.retrieveAllUsersWithRolesExcel(response);

        // Assert
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());

        byte[] excelBytes = response.getContentAsByteArray();
        assertTrue(excelBytes.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Users");
            assertNotNull(sheet);
            // Only header row should be present
            assertNotNull(sheet.getRow(0));
            assertNull(sheet.getRow(1));
        }
    }

    @DisplayName("retrieveAllUsersWithRolesExcel handles user with null roles gracefully")
    @Test
    void test_retrieve_all_users_with_roles_excel_null_roles() throws IOException {
        // Arrange
        AppUser user = new AppUser();
        user.setId(3L);
        user.setUsername("charlie");
        user.setRoles(null);

        when(appuserRepository.findAll()).thenReturn(Collections.singletonList(user));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act
        usersApi.retrieveAllUsersWithRolesExcel(response);

        // Assert
        byte[] excelBytes = response.getContentAsByteArray();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheet("Users");
            Row row = sheet.getRow(1);
            assertNotNull(row);
            assertEquals("charlie", row.getCell(1).getStringCellValue());
            assertEquals("", row.getCell(2).getStringCellValue());
        }
    }

    @DisplayName("retrieveAllUsersWithRolesExcel throws exception when READ_USER permission is missing")
    @Test
    void test_retrieve_all_users_with_roles_excel_missing_user_permission() {
        // Arrange
        doThrow(new NoAuthorizationException("User has no authority to READ users"))
                .when(connectedUser).validateHasReadPermission("USER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act & Assert
        assertThrows(NoAuthorizationException.class,
                () -> usersApi.retrieveAllUsersWithRolesExcel(response));
        verify(appuserRepository, never()).findAll();
    }

    @DisplayName("retrieveAllUsersWithRolesExcel throws exception when READ_ROLE permission is missing")
    @Test
    void test_retrieve_all_users_with_roles_excel_missing_role_permission() {
        // Arrange
        doNothing().when(connectedUser).validateHasReadPermission("USER");
        doThrow(new NoAuthorizationException("User has no authority to READ roles"))
                .when(connectedUser).validateHasReadPermission("ROLE");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act & Assert
        assertThrows(NoAuthorizationException.class,
                () -> usersApi.retrieveAllUsersWithRolesExcel(response));
        verify(appuserRepository, never()).findAll();
    }


    @DisplayName("retrieveUserPermissionsByUsername delegates to userService when user fetches own profile")
    @Test
    void test_retrieve_permissions_by_own_email_skips_permission_check() {
        // Arrange
        String email = "kelvin.thuku@oneacrefund.org";
        when(connectedUser.getEmail()).thenReturn(email);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AppUserDto userModel = new AppUserDto();
        AppUser appUser = new AppUser();
        appUser.setUsername("kelvin.thuku");
        appUser.setEmail(email);
        userModel = new AppUserDto(appUser,Set.of(), Set.of());
        when(userService.retrieveUserByUsername(email, response)).thenReturn(userModel);

        // Act
        AppUserDto dto = usersApi.retrieveUserPermissionsByUsername(email, response);

        // Assert
        assertNotNull(dto);
        assertEquals("kelvin.thuku", dto.getUsername());
        verify(connectedUser, never()).validateHasReadPermission("USER");
        verify(userService).retrieveUserByUsername(email, response);
    }

    @DisplayName("retrieveUserPermissionsByUsername requires READ_USER permission when fetching another user")
    @Test
    void test_retrieve_permissions_for_other_user_requires_permission() {
        // Arrange
        String requestedUser = "other.user@oneacrefund.org";
        when(connectedUser.getEmail()).thenReturn("admin@oneacrefund.org");
        doNothing().when(connectedUser).validateHasReadPermission("USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userService.retrieveUserByUsername(requestedUser, response)).thenReturn(new AppUserDto());

        // Act
        AppUserDto dto = usersApi.retrieveUserPermissionsByUsername(requestedUser, response);

        // Assert
        assertNotNull(dto);
        verify(connectedUser, times(1)).validateHasReadPermission("USER");
        verify(userService).retrieveUserByUsername(requestedUser, response);
    }

    @DisplayName("retrieveUserPermissionsByUsername throws NoAuthorizationException when caller lacks READ_USER permission")
    @Test
    void test_retrieve_permissions_for_other_user_throws_when_unauthorized() {
        // Arrange
        when(connectedUser.getEmail()).thenReturn("admin@oneacrefund.org");
        doThrow(new NoAuthorizationException("No READ_USER permission"))
                .when(connectedUser).validateHasReadPermission("USER");

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Act & Assert
        assertThrows(NoAuthorizationException.class,
                () -> usersApi.retrieveUserPermissionsByUsername("other.user@oneacrefund.org", response));
        verify(userService, never()).retrieveUserByUsername(any(), any());
    }

    @DisplayName("retrieveUserPermissionsByUsername returns null when service cannot find user")
    @Test
    void test_retrieve_permissions_user_not_found() {
        // Arrange
        String email = "unknown@oneacrefund.org";
        when(connectedUser.getEmail()).thenReturn(email);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(userService.retrieveUserByUsername(email, response)).thenReturn(null);

        // Act
        AppUserDto dto = usersApi.retrieveUserPermissionsByUsername(email, response);

        // Assert
        assertNull(dto);
        verify(userService).retrieveUserByUsername(email, response);
    }

}