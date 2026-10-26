package org.apache.fineract.config.security;

import org.apache.fineract.ServerApplication;
import org.apache.fineract.config.security.filter.TenantAwareKeycloakFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real security chain followed by the real corsFilter, in the same order
 * as the servlet container (security at -100, cors at security.filter-order - 5).
 */
class SecurityConfigCorsTest {

    private static final String ORIGIN = "https://ph-web.qa.oneacrefund.org";
    private static final String PERMISSIONS_URL = "/api/v1/users/{email}/username";
    private static final String EMAIL = "someone@oneacrefund.org";

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();

        ServerApplication app = new ServerApplication();
        ReflectionTestUtils.setField(app, "securityFilterOrder", 4);
        CorsFilter corsFilter = app.corsFilter().getFilter();

        DelegatingFilterProxy securityChain = new DelegatingFilterProxy("springSecurityFilterChain", context);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityChain, corsFilter)
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("Preflight without a token is answered with CORS headers")
    void preflightIsAnswered() throws Exception {
        mockMvc.perform(options(PERMISSIONS_URL, EMAIL)
                        .header("Origin", ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,platform-tenantid"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Real requests without a token are still rejected")
    void getWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get(PERMISSIONS_URL, EMAIL).header("Origin", ORIGIN))
                .andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }

        @Bean
        TenantAwareKeycloakFilter tenantAwareKeycloakFilter() throws Exception {
            TenantAwareKeycloakFilter filter = mock(TenantAwareKeycloakFilter.class);
            doAnswer(invocation -> {
                ((FilterChain) invocation.getArgument(2)).doFilter(
                        invocation.<HttpServletRequest>getArgument(0),
                        invocation.<HttpServletResponse>getArgument(1));
                return null;
            }).when(filter).doFilter(any(), any(), any());
            return filter;
        }
    }
}
