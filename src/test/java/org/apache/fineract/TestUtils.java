package org.apache.fineract;

import org.apache.fineract.organisation.user.AppUser;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    public void setupSecurityContext(AppUser appUser) {
        AccessToken accessToken = mock(AccessToken.class);
        when(accessToken.getPreferredUsername()).thenReturn(appUser.getUsername());
        KeycloakSecurityContext keycloakSecurityContext = mock(KeycloakSecurityContext.class);
        when(keycloakSecurityContext.getToken()).thenReturn(accessToken);
        KeycloakPrincipal<KeycloakSecurityContext> keycloakPrincipal =
                new KeycloakPrincipal<>("mock", keycloakSecurityContext);
        Authentication authentication =
                new PreAuthenticatedAuthenticationToken(keycloakPrincipal, "token", null);
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
