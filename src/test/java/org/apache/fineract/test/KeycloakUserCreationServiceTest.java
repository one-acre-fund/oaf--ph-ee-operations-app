package org.apache.fineract.test;

import org.apache.fineract.config.security.service.KeycloakUserCreationService;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeycloakUserCreationServiceTest {
    private AutoCloseable mocks;

    private AppUserRepository appUserRepository;
    private PasswordEncoder passwordEncoder;

    private KeycloakUserCreationService service;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        appUserRepository = mock(AppUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new KeycloakUserCreationService(appUserRepository, passwordEncoder);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createUserFromKeycloakUserData returns null when authentication missing")
    void createUserFromKeycloakUserData_nullAuth_returnsNull() {
        SecurityContextHolder.clearContext();
        AppUser result = service.createUserFromKeycloakUserData(null);
        assertNull(result);
        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createUserFromKeycloakUserData returns null when principal not Jwt")
    void createUserFromKeycloakUserData_nonJwtPrincipal_returnsNull() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("stringPrincipal", null));
        AppUser result = service.createUserFromKeycloakUserData(SecurityContextHolder.getContext().getAuthentication());
        assertNull(result);
        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createUserFromKeycloakUserData returns null when claims are null")
    void createUserFromKeycloakUserData_nullClaims_returnsNull() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));
        AppUser result = service.createUserFromKeycloakUserData(SecurityContextHolder.getContext().getAuthentication());
        assertNull(result);
        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createUserFromKeycloakUserData saves user built from JWT claims and encodes password")
    void createUserFromKeycloakUserData_happyPath_savesUser() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", "john.doe@oneacrefund.org");
        claims.put("email", "john.doe@oneacrefund.org");
        claims.put("given_name", "John");
        claims.put("family_name", "Doe");
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(claims);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

        when(passwordEncoder.encode(any(String.class))).thenAnswer(inv -> "ENC(" + inv.getArgument(0, String.class) + ")");
        when(appUserRepository.saveAndFlush(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0, AppUser.class));

        AppUser created = service.createUserFromKeycloakUserData(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(created);
        assertEquals("john.doe@oneacrefund.org", created.getUsername());
        assertEquals("john.doe@oneacrefund.org", created.getEmail());
        assertEquals("John", created.getFirstname());
        assertEquals("Doe", created.getLastname());
        assertTrue(created.isEnabled());
        assertTrue(created.isAccountNonExpired());
        assertTrue(created.isAccountNonLocked());
        assertTrue(created.isCredentialsNonExpired());
        assertFalse(created.isDeleted());
        assertNotNull(created.getLastTimePasswordUpdated());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(captor.capture());
        assertNotNull(captor.getValue().getPassword());
        assertTrue(captor.getValue().getPassword().startsWith("ENC("));
    }

    @Test
    @DisplayName("getAppUser builds AppUser with encoded password and fields set")
    void getAppUser_buildsUser_correctFields() throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "alice@oneacrefund.org");
        claims.put("given_name", "Alice");
        claims.put("family_name", "Smith");
        when(passwordEncoder.encode(any(String.class))).thenAnswer(inv -> "ENC(" + inv.getArgument(0, String.class) + ")");

        Method m = KeycloakUserCreationService.class.getDeclaredMethod("getAppUser", Map.class, String.class, String.class);
        m.setAccessible(true);
        AppUser user = (AppUser) m.invoke(service, claims, "alice@oneacrefund.org", "rawPass");

        assertEquals("alice@oneacrefund.org", user.getUsername());
        assertEquals("alice@oneacrefund.org", user.getEmail());
        assertEquals("Alice", user.getFirstname());
        assertEquals("Smith", user.getLastname());
        assertFalse(user.isDeleted());
        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertNotNull(user.getLastTimePasswordUpdated());
        assertNotNull(user.getCreatedDate());
        assertNotNull(user.getLastModifiedDate());
        assertTrue(user.getCreatedDate().isBefore(LocalDateTime.now().plusSeconds(2)));
        assertTrue(user.getLastModifiedDate().isBefore(LocalDateTime.now().plusSeconds(2)));
        assertTrue(user.getLastTimePasswordUpdated().before(new Date(System.currentTimeMillis() + 2000)));
    }
}

