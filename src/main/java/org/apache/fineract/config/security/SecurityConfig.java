package org.apache.fineract.config.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.fineract.config.security.filter.TenantAwareKeycloakFilter;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
@Configuration
@EnableWebSecurity
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {

    @Autowired
    private TenantAwareKeycloakFilter tenantAwareKeycloakFilter;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(keycloakAuthenticationProvider());
    }

    /**
     * Prevent Spring Boot from auto-registering TenantAwareKeycloakFilter
     * as a standalone servlet filter. It must only run inside the Spring
     * Security chain (added below), so that permitAll() rules gate it.
     */
    @Bean
    public FilterRegistrationBean<TenantAwareKeycloakFilter> tenantFilterRegistration(
            TenantAwareKeycloakFilter filter) {
        FilterRegistrationBean<TenantAwareKeycloakFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
                .ignoringAntMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/swagger-config/**",
                        "/api/v1/errorcode/**",
                        "/actuator/**"
                )
                .and()
                .authorizeRequests()
                .antMatchers(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/swagger-config/**",
                        "/api/v1/errorcode/**",
                        "/actuator/**"
                ).permitAll()
                .anyRequest().authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .oauth2ResourceServer()
                .jwt()
                .and()
                .and()
                // Register the filter inside the security chain, after JWT auth resolves.
                // It will NOT run for permitAll() paths because shouldNotFilter() gates those.
                .addFilterAfter(tenantAwareKeycloakFilter, BearerTokenAuthenticationFilter.class);
    }

    @Bean
    public KeycloakConfigResolver keycloakConfigResolver() {
        return new KeycloakSpringBootConfigResolver();
    }

    @Bean
    @Override
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new NullAuthenticatedSessionStrategy();
    }

    @Bean
    public OperationCustomizer globalTenantHeader() {
        return (operation, handlerMethod) -> {
            Parameter tenantHeader = new HeaderParameter()
                    .name("Platform-TenantId")
                    .required(true)
                    .description("Tenant identifier header (e.g., 'oaf')")
                    .schema(new StringSchema());

            operation.addParametersItem(tenantHeader);
            return operation;
        };
    }
}


