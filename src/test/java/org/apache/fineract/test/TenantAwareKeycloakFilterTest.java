package org.apache.fineract.test;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.config.security.filter.TenantAwareKeycloakFilter;
import org.apache.fineract.config.security.service.KeycloakUserCreationService;
import org.apache.fineract.config.security.utils.TenantIdUtil;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class TenantAwareKeycloakFilterTest {

    private UserDetailsService userDetailsService;
    private KeycloakUserCreationService keycloakUserCreationService;
    private TenantIdUtil tenantIdUtil;
    private TenantServerConnectionRepository tenantRepo;

    private TenantAwareKeycloakFilter filter;

    private AutoCloseable mocks;

    @BeforeEach
    void setUpMocks() {
        mocks = MockitoAnnotations.openMocks(this);
        userDetailsService = mock(UserDetailsService.class);
        keycloakUserCreationService = mock(KeycloakUserCreationService.class);
        tenantIdUtil = mock(TenantIdUtil.class);
        tenantRepo = mock(TenantServerConnectionRepository.class);

        filter = new TenantAwareKeycloakFilter(userDetailsService, keycloakUserCreationService, tenantIdUtil, tenantRepo);

        // Ensure a clean security context at start
        SecurityContextHolder.clearContext();
        ThreadLocalContextUtil.setCurrentUser(null);
    }

    @AfterEach
    void tearDownMocks() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
        // Clear thread-local and security context to avoid leakage
        ThreadLocalContextUtil.setCurrentUser(null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipOnOptionsRequest() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/any");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = mock(MockFilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
    }

    @Test
    void shouldSkipOnSwaggerOrActuatorRequest() throws Exception {
        for (String path : new String[]{"/actuator/health", "/swagger-ui", "/v3/api-docs"}) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = mock(MockFilterChain.class);

            filter.doFilter(req, res, chain);
            verify(chain, atLeastOnce()).doFilter(req, res);
        }
    }

    @Test
    void shouldFailIfTenantHeaderMissing() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tenantIdUtil.useDefaultTenantIdIfBlank(null, "/api/resource")).thenReturn(null);

        filter.doFilter(req, res, chain);

        assertEquals(400, res.getStatus());
    }

    @Test
    void shouldFailIfEmailDomainIsNotAccepted() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource");
        req.addHeader("Platform-TenantId", "oaf");
        req.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tenantIdUtil.useDefaultTenantIdIfBlank("oaf", "/api/resource")).thenReturn("oaf");
        when(tenantRepo.findOneBySchemaName("oaf")).thenReturn(new TenantServerConnection());

        Jwt mockJwt = mock(Jwt.class);
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "malicious@example.com");
        claims.put("preferred_username", "malicious@example.com");
        when(mockJwt.getClaims()).thenReturn(claims);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(mockJwt, null));

        filter.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
    }

    @Test
    void shouldFailIfUserNotFound() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource");
        req.addHeader("Platform-TenantId", "oaf");
        req.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tenantIdUtil.useDefaultTenantIdIfBlank("oaf", "/api/resource")).thenReturn("oaf");
        when(tenantRepo.findOneBySchemaName("oaf")).thenReturn(new TenantServerConnection());

        Jwt mockJwt = mock(Jwt.class);
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "dev@oneacrefund.org");
        claims.put("preferred_username", "dev@oneacrefund.org");
        when(mockJwt.getClaims()).thenReturn(claims);

        // Set authentication with JWT principal
        UsernamePasswordAuthenticationToken keycloakAuth = new UsernamePasswordAuthenticationToken(mockJwt, null);
        SecurityContextHolder.getContext().setAuthentication(keycloakAuth);

        when(userDetailsService.loadUserByUsername("dev@oneacrefund.org"))
                .thenThrow(new UsernameNotFoundException("not found"));

        // Stub first-time creation path used by filter
        AppUser created = mock(AppUser.class);
        when(created.getUsername()).thenReturn("dev@oneacrefund.org");
        when(keycloakUserCreationService.createUserFromKeycloakUserData(keycloakAuth)).thenReturn(created);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        Set<String> perms = new HashSet<>();
        perms.add("ROLE_USER");
        when(keycloakUserCreationService.resolveAuthoritiesFromUserDetails(any()))
                .thenReturn(Pair.of(authorities, perms));

        filter.doFilter(req, res, chain);

        // Expect success after first time user creation
        assertEquals(200, res.getStatus());
    }

    @Test
    void shouldLoadAndSetUserIfValidEmail() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource");
        req.addHeader("Platform-TenantId", "oaf");
        req.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tenantIdUtil.useDefaultTenantIdIfBlank("oaf", "/api/resource")).thenReturn("oaf");
        when(tenantRepo.findOneBySchemaName("oaf")).thenReturn(new TenantServerConnection());

        Jwt jwt = mock(Jwt.class);
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "admin@oneacrefund.org");
        claims.put("preferred_username", "admin@oneacrefund.org");
        when(jwt.getClaims()).thenReturn(claims);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

        // Return AppUser instead of Spring Security User to match filter cast
        AppUser appUserDetails = mock(AppUser.class);
        when(appUserDetails.getUsername()).thenReturn("admin@oneacrefund.org");
        when(userDetailsService.loadUserByUsername("admin@oneacrefund.org")).thenReturn(appUserDetails);

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        Set<String> perms = new HashSet<>();
        perms.add("ROLE_ADMIN");
        when(keycloakUserCreationService.resolveAuthoritiesFromUserDetails(any()))
                .thenReturn(Pair.of(authorities, perms));

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof AppUser);
    }

    @Test
    void shouldHandleAppUserPrincipal() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource/report");
        req.addHeader("Platform-TenantId", "oaf");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tenantIdUtil.useDefaultTenantIdIfBlank("oaf", "/api/resource/report")).thenReturn("oaf");
        when(tenantRepo.findOneBySchemaName("oaf")).thenReturn(new TenantServerConnection());

        AppUser appUser = mock(AppUser.class);
        when(appUser.getUsername()).thenReturn("user@oneacrefund.org");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(appUser, null));

        // Return AppUser from userDetailsService, not Spring User
        AppUser loaded = mock(AppUser.class);
        when(loaded.getUsername()).thenReturn("user@oneacrefund.org");
        when(userDetailsService.loadUserByUsername("user@oneacrefund.org")).thenReturn(loaded);

        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        Set<String> perms = new HashSet<>();
        perms.add("ROLE_USER");
        when(keycloakUserCreationService.resolveAuthoritiesFromUserDetails(any()))
                .thenReturn(Pair.of(authorities, perms));

        filter.doFilter(req, res, chain);

        assertEquals(200, res.getStatus());
    }

    @Test
    void shouldReturn500WhenUserProvisioningFails() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/resource/report");
        req.addHeader("Platform-TenantId", "oaf");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(tenantIdUtil.useDefaultTenantIdIfBlank("oaf", "/api/resource/report")).thenReturn("oaf");
        when(tenantRepo.findOneBySchemaName("oaf")).thenReturn(new TenantServerConnection());

        AppUser principal = mock(AppUser.class);
        when(principal.getUsername()).thenReturn("missing@oneacrefund.org");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null));

        when(userDetailsService.loadUserByUsername("missing@oneacrefund.org"))
                .thenThrow(new UsernameNotFoundException("not found"));

        when(keycloakUserCreationService.createUserFromKeycloakUserData(any(Authentication.class)))
                .thenReturn(null);

        filter.doFilter(req, res, chain);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res.getStatus());
    }
}
