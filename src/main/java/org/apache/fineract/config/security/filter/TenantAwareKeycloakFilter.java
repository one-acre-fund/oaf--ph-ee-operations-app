/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.config.security.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import org.apache.fineract.config.security.exception.InvalidTenantIdentifierException;
import org.apache.fineract.config.security.service.KeycloakUserCreationService;
import org.apache.fineract.config.security.utils.TenantIdUtil;
import org.apache.fineract.core.service.PlatformRequestLog;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

import static org.apache.fineract.config.security.utils.SecurityUtils.extractUsername;

@Service
@Profile("keycloak")
@Slf4j
public class TenantAwareKeycloakFilter extends OncePerRequestFilter {

    private static final String TENANT_REQUEST_HEADER = "Platform-TenantId";
    private static final boolean EXCEPTION_IF_HEADER_MISSING = true;
    private final UserDetailsService userDetailsService;
    private final TenantIdUtil tenantIdUtil;
    private final KeycloakUserCreationService keycloakUserCreationService;
    private final TenantServerConnectionRepository repository;

    @Autowired
    public TenantAwareKeycloakFilter(final UserDetailsService userDetailsService,
                                     final KeycloakUserCreationService keycloakUserCreationService,
                                     TenantIdUtil tenantIdUtil, TenantServerConnectionRepository repository) {
        this.userDetailsService = userDetailsService;
        this.keycloakUserCreationService = keycloakUserCreationService;
        this.tenantIdUtil = tenantIdUtil;
        this.repository = repository;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final StopWatch task = new StopWatch();
        task.start();

        try {
            if (isPreflightOrInternalRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String tenantIdentifier = resolveTenantIdentifier(request);
            validateTenantIdentifier(tenantIdentifier);

            ThreadLocalContextUtil.setTenant(this.repository.findOneBySchemaName(tenantIdentifier));
            setAuthTokenIfPresent(request);

            logRequestHeaders(request);

            if (!authenticateAndSetUser(response)) {
                return;
            }

            filterChain.doFilter(request, response);

        } catch (final InvalidTenantIdentifierException e) {
            handleInvalidTenantException(response, e);
        } finally {
            task.stop();
            ThreadLocalContextUtil.clear();
            logger.info(PlatformRequestLog.from(task, request).toString());
        }
    }

    private boolean isPreflightOrInternalRequest(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || url.contains("actuator")
                || url.contains("api-docs")
                || url.contains("swagger");
    }

    private String resolveTenantIdentifier(HttpServletRequest request) {
        String tenantIdentifier = request.getHeader(TENANT_REQUEST_HEADER);
        if (StringUtils.isBlank(tenantIdentifier)) {
            tenantIdentifier = request.getParameter("tenantIdentifier");
        }
        return tenantIdUtil.useDefaultTenantIdIfBlank(tenantIdentifier, request.getRequestURI());
    }

    private void validateTenantIdentifier(String tenantIdentifier) {
        if ((tenantIdentifier == null || tenantIdentifier.isEmpty()) && EXCEPTION_IF_HEADER_MISSING) {
            throw new InvalidTenantIdentifierException("No tenant identifier found: Add request header of '"
                    + TENANT_REQUEST_HEADER + "' or add the parameter 'tenantIdentifier' to query string of request URL.");
        }
    }

    private void setAuthTokenIfPresent(HttpServletRequest request) {
        String authToken = request.getHeader("Authorization");
        if (authToken != null && authToken.startsWith("Bearer ")) {
            ThreadLocalContextUtil.setAuthToken(authToken.replaceFirst("Bearer ", ""));
        }
    }

    private void logRequestHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        log.debug("Logging all the request headers for troubleshooting:");
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!"Authorization".equalsIgnoreCase(headerName) && !"Cookie".equalsIgnoreCase(headerName)) {
                    log.debug("{}: {}", headerName, request.getHeader(headerName));
                }
            }
        }
    }

    private boolean authenticateAndSetUser(HttpServletResponse response) throws IOException {
        Authentication keycloakAuth = SecurityContextHolder.getContext().getAuthentication();
        if (keycloakAuth == null) {
            return true; // Let Keycloak handle auth failure elsewhere
        }

        try {
            String username = extractUsername(keycloakAuth);
            if (!username.endsWith("@oneacrefund.org")) {
                log.error("User not recognized: {}", username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authorized to access this resource");
                return false;
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails != null) {
                ThreadLocalContextUtil.setCurrentUser((AppUser) userDetails);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        userDetails.getPassword(),
                        keycloakUserCreationService.resolveAuthoritiesFromUserDetails(userDetails).getLeft()
                );
                SecurityContextHolder.clearContext();
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (UsernameNotFoundException ex) {
            log.info("Keycloak user not found in database: {}. Proceeding to create as first time login user", ex.getMessage());
            AppUser appUser = keycloakUserCreationService
                    .createUserFromKeycloakUserData(keycloakAuth);
            if (appUser == null) {
                log.error("Failed to create user from Keycloak data");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to provision user");
                return false;
            }
            ThreadLocalContextUtil.setCurrentUser(appUser);
            Authentication authentication = new UsernamePasswordAuthenticationToken(appUser, appUser.getPassword(),
                    keycloakUserCreationService.resolveAuthoritiesFromUserDetails(appUser).getLeft());
            SecurityContextHolder.clearContext();
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        return true;
    }

    private void handleInvalidTenantException(HttpServletResponse response, InvalidTenantIdentifierException e)
            throws IOException {
        SecurityContextHolder.getContext().setAuthentication(null);
        ThreadLocalContextUtil.clear();
        log.error("Invalid tenant identifier exception occurred: {}", e.getMessage());
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant identifier.");
    }

}
