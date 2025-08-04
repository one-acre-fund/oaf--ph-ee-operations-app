package org.apache.fineract.test;

import org.apache.fineract.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.mockito.InjectMocks;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void testKeycloakConfigResolverBean() {
        KeycloakConfigResolver resolver = securityConfig.keycloakConfigResolver();
        assertNotNull(resolver);
        assertTrue(resolver instanceof KeycloakSpringBootConfigResolver);
    }

    @Test
    void testSessionAuthenticationStrategyBean() {
        SessionAuthenticationStrategy strategy = securityConfig.sessionAuthenticationStrategy();
        assertNotNull(strategy);
        assertTrue(strategy instanceof NullAuthenticatedSessionStrategy);
    }
}
