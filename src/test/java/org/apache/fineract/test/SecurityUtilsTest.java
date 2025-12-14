package org.apache.fineract.test;

import org.apache.fineract.config.security.utils.SecurityUtils;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityUtilsTest {

    @Test
    @DisplayName("extractUsername returns AppUser.username when principal is AppUser")
    void returnsUsernameFromAppUserPrincipal() {
        AppUser appUser = mock(AppUser.class);
        when(appUser.getUsername()).thenReturn("user@oneacrefund.org");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(appUser);

        String result = SecurityUtils.extractUsername(auth);
        assertEquals("user@oneacrefund.org", result);
    }

    @Test
    @DisplayName("extractUsername returns preferred_username claim when principal is Jwt")
    void returnsPreferredUsernameFromJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("preferred_username", "dev@oneacrefund.org");
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(claims);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        String result = SecurityUtils.extractUsername(auth);
        assertEquals("dev@oneacrefund.org", result);
    }

    @Test
    @DisplayName("extractUsername returns empty string when Jwt lacks preferred_username")
    void returnsEmptyWhenJwtMissingPreferredUsername() {
        Map<String, Object> claims = new HashMap<>();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(claims);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);

        String result = SecurityUtils.extractUsername(auth);
        assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    @DisplayName("extractUsername returns empty string when principal is null")
    void returnsEmptyWhenPrincipalNull() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(null);

        String result = SecurityUtils.extractUsername(auth);
        assertEquals("", result);
    }

    @Test
    @DisplayName("extractUsername returns empty string for unsupported principal types")
    void returnsEmptyForUnsupportedPrincipal() {
        Object otherPrincipal = new Object();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(otherPrincipal);

        String result = SecurityUtils.extractUsername(auth);
        assertEquals("", result);
    }
}

