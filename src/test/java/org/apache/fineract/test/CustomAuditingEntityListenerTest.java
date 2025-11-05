package org.apache.fineract.test;

import org.apache.fineract.config.BeanUtil;
import org.apache.fineract.config.CustomAuditingEntityListener;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class CustomAuditingEntityListenerTest {
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;
    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    public void testGetCurrentUser_authenticatedKeycloakPrincipal_returnsAppUser() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        AppUser expectedUser = mock(AppUser.class);
        KeycloakSecurityContext keycloakSecurityContext = mock(KeycloakSecurityContext.class);
        org.keycloak.representations.AccessToken token = mock(org.keycloak.representations.AccessToken.class);
        when(token.getPreferredUsername()).thenReturn("testuser");
        when(keycloakSecurityContext.getToken()).thenReturn(token);
        KeycloakPrincipal<KeycloakSecurityContext> principal = mock(KeycloakPrincipal.class);
        when(principal.getKeycloakSecurityContext()).thenReturn(keycloakSecurityContext);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(appUserRepository.findAppUserByName("testuser")).thenReturn(expectedUser);
        try (MockedStatic<SecurityContextHolder> schMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            schMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            beanUtilMock.when(() -> BeanUtil.getBean(AppUserRepository.class)).thenReturn(appUserRepository);
            AppUser result = listener.getCurrentUser();
            assertEquals(expectedUser, result);
        }
    }

    @Test
    public void testGetCurrentUser_nullAuthentication_returnsNull() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        try (MockedStatic<SecurityContextHolder> schMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            schMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(null);
            beanUtilMock.when(() -> BeanUtil.getBean(AppUserRepository.class)).thenReturn(appUserRepository);
            AppUser result = listener.getCurrentUser();
            assertNull(result);
        }
    }

    @Test
    public void testGetCurrentUser_nonKeycloakPrincipal_returnsNull() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        when(authentication.getPrincipal()).thenReturn("notKeycloakPrincipal");
        try (MockedStatic<SecurityContextHolder> schMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            schMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            beanUtilMock.when(() -> BeanUtil.getBean(AppUserRepository.class)).thenReturn(appUserRepository);
            AppUser result = listener.getCurrentUser();
            assertNull(result);
        }
    }

    @Test
    public void testGetCurrentUser_keycloakPrincipalMissingUsername_returnsNull() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        KeycloakSecurityContext keycloakSecurityContext = mock(KeycloakSecurityContext.class);
        org.keycloak.representations.AccessToken token = mock(org.keycloak.representations.AccessToken.class);
        when(token.getPreferredUsername()).thenReturn(null);
        when(keycloakSecurityContext.getToken()).thenReturn(token);
        KeycloakPrincipal<KeycloakSecurityContext> principal = mock(KeycloakPrincipal.class);
        when(principal.getKeycloakSecurityContext()).thenReturn(keycloakSecurityContext);
        when(authentication.getPrincipal()).thenReturn(principal);
        try (MockedStatic<SecurityContextHolder> schMock = mockStatic(SecurityContextHolder.class);
             MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            schMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            beanUtilMock.when(() -> BeanUtil.getBean(AppUserRepository.class)).thenReturn(appUserRepository);
            AppUser result = listener.getCurrentUser();
            assertNull(result);
        }
    }
}

