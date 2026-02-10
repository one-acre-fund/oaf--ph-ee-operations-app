package org.apache.fineract.test;

import org.apache.fineract.api.UsersApi;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

class UsersApiTest {

    @Mock
    private AppUserRepository appUserRepository;

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
    void testDeactivateUser() {
        // Create a sample AppUser
        AppUser sampleUser = new AppUser();
        sampleUser.setId(1L);
        sampleUser.setEnabled(true);
        // Create a separate instance for disabledUser
        AppUser disabledUser = new AppUser();
        disabledUser.setId(1L);
        disabledUser.setEnabled(false);

        // Mock the behavior of appUserRepository.findById
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenReturn(disabledUser);

        // Invoke the method
        AppUser user = usersApi.deactivate(1L, mock(HttpServletResponse.class));
        verify(appUserRepository, times(1)).saveAndFlush(disabledUser);
        assertFalse(user.isEnabled());
    }

    @Test
    void testActivateUser() {
        // Create a sample AppUser
        AppUser sampleUser = new AppUser();
        sampleUser.setId(1L);
        sampleUser.setEnabled(false);
        // Create a separate instance for disabledUser
        AppUser enabledUser = new AppUser();
        enabledUser.setId(1L);
        enabledUser.setEnabled(true);

        // Mock the behavior of appUserRepository.findById
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenReturn(enabledUser);

        // Invoke the method
        AppUser user = usersApi.activate(1L, mock(HttpServletResponse.class));
        verify(appUserRepository, times(1)).saveAndFlush(enabledUser);
        assertEquals(user.isEnabled(), enabledUser.isEnabled());
    }

    @Test
    void testActivateUserNotFound() {
        // Mock the behavior of appUserRepository.findById to return an empty Optional
        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        // Invoke the method
        AppUser activatedUser = usersApi.activate(1L, mock(HttpServletResponse.class));

        // Assertions
        assertNull(activatedUser);
    }

    @Test
    @DisplayName("Test that assigning user currencies works")
    void testUserCurrenciesAssignment() {
        Long userId = 1L;
        List<String> currencies = Arrays.asList("USD", "EUR");

        AppUser existingUser = new AppUser();
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        HttpServletResponse response = mock(HttpServletResponse.class);

        usersApi.userCurrenciesAssignment(userId, currencies, response);

        verify(appUserRepository, times(1)).findById(userId);
        verify(appUserRepository, times(1)).saveAndFlush(existingUser);

    }

    @Test
    @DisplayName("Test that assigning user PayeePartyIds works")
    void testUserPayeePartyIdsAssignment() {
        Long userId = 1L;
        List<String> payeePartyIds = Arrays.asList("12345", "67890");

        AppUser existingUser = new AppUser();
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        HttpServletResponse response = mock(HttpServletResponse.class);

        usersApi.userPayeePartyIdsAssignment(userId, payeePartyIds, response);

        verify(appUserRepository, times(1)).findById(userId);
        verify(appUserRepository, times(1)).saveAndFlush(existingUser);
    }
}

