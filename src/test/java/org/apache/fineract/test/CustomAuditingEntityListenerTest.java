package org.apache.fineract.test;

import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.events.NewAuditEvent;
import org.apache.fineract.audit.service.AuditService;
import org.apache.fineract.config.BeanUtil;
import org.apache.fineract.config.CustomAuditingEntityListener;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

class CustomAuditingEntityListenerTest {
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    void testGetCurrentUser_authenticatedKeycloakPrincipal_returnsAppUser() {
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
    void testGetCurrentUser_nullAuthentication_returnsNull() {
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
    void testGetCurrentUser_nonKeycloakPrincipal_returnsNull() {
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
    void testGetCurrentUser_keycloakPrincipalMissingUsername_returnsNull() {
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

    @Test
    void testLogAction_entityIsAuditSource_returnsImmediately() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        AuditService auditService = mock(AuditService.class);
        AuditSource auditSource = mock(AuditSource.class);
        try (MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            beanUtilMock.when(() -> BeanUtil.getBean(AuditService.class)).thenReturn(auditService);
            listener.logAction("CREATE", auditSource);
            verify(auditService, never()).createNewEntry(any(NewAuditEvent.class));
        }
    }

    @Test
    void testLogAction_validEntity_createsAuditEventSuccessfully() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        AuditService auditService = mock(AuditService.class);
        Object entity = new Object();

        try (MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            beanUtilMock.when(() -> BeanUtil.getBean(AuditService.class)).thenReturn(auditService);

            listener.logAction("UPDATE", entity);

            // Verify that an audit event was created successfully
            verify(auditService).createNewEntry(any(NewAuditEvent.class));
        }
    }

    @Test
    void testLogAction_beanUtilThrowsException_logsError() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        Object entity = new Object();

        try (MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            // Simulate an exception when retrieving the bean
            beanUtilMock.when(() -> BeanUtil.getBean(AuditService.class))
                    .thenThrow(new RuntimeException("Bean not found"));

            // Should catch and log exception, not propagate
            listener.logAction("DELETE", entity);
        }
    }

    @Test
    void testLogAction_auditServiceThrowsException_logsError() {
        CustomAuditingEntityListener listener = new CustomAuditingEntityListener();
        AuditService auditService = mock(AuditService.class);
        Object entity = new Object();

        try (MockedStatic<BeanUtil> beanUtilMock = mockStatic(BeanUtil.class)) {
            beanUtilMock.when(() -> BeanUtil.getBean(AuditService.class)).thenReturn(auditService);

            // Simulate exception thrown by the service
            doThrow(new RuntimeException("DB error")).when(auditService).createNewEntry(any(NewAuditEvent.class));

            // Execute (should not throw)
            assertDoesNotThrow(() -> listener.logAction("SAVE", entity));

            // ✅ Assert that the service was indeed called once
            verify(auditService, times(1)).createNewEntry(any(NewAuditEvent.class));
        }
    }


}

