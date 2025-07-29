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

import org.apache.commons.lang3.time.StopWatch;

import org.apache.fineract.config.security.exception.InvalidTenantIdentifierException;
import org.apache.fineract.config.security.service.KeycloakUserCreationService;
import org.apache.fineract.config.security.utils.TenantIdUtil;
import org.apache.fineract.core.service.PlatformRequestLog;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.apache.fineract.organisation.user.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;


@Service
@Profile("keycloak")
public class TenantAwareKeycloakFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TenantAwareKeycloakFilter.class);
    private final String tenantRequestHeader = "Platform-TenantId";
    private final boolean exceptionIfHeaderMissing = true;
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

            if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURL().toString().contains("actuator") ||
                    request.getRequestURL().toString().contains("api-docs") || request.getRequestURL().toString().contains("swagger")) {
                // ignore to allow 'preflight' requests from AJAX applications
                // in different origin (domain name)
            } else {

                String tenantIdentifier = request.getHeader(this.tenantRequestHeader);

                if (org.apache.commons.lang3.StringUtils.isBlank(tenantIdentifier)) {
                    tenantIdentifier = request.getParameter("tenantIdentifier");
                }

                tenantIdentifier = tenantIdUtil.useDefaultTenantIdIfBlank(tenantIdentifier, request.getRequestURI());

                if (tenantIdentifier == null || tenantIdentifier.isEmpty() && this.exceptionIfHeaderMissing) {
                    throw new InvalidTenantIdentifierException("No tenant identifier found: Add request header of '"
                            + this.tenantRequestHeader + "' or add the parameter 'tenantIdentifier' to query string of request URL.");
                }

                String pathInfo = request.getRequestURI();
                boolean isReportRequest = false;
                if (pathInfo != null && pathInfo.contains("report")) {
                    isReportRequest = true;
                }

                ThreadLocalContextUtil.setTenant(this.repository.findOneBySchemaName(tenantIdentifier));

                String authToken = request.getHeader("Authorization");

                Enumeration<String> headerNames = request.getHeaderNames();
                LOG.debug("Logging all the request headers for troubleshooting:");
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        String headerName = headerNames.nextElement();
                        if (!headerName.equalsIgnoreCase("Authorization") &&
                             !headerName.equalsIgnoreCase("Cookie")) {
                            LOG.debug("{}: {}", headerName, request.getHeader(headerName));
                        }
                    }
                }

                if (authToken != null && authToken.startsWith("Bearer ")) {
                    ThreadLocalContextUtil.setAuthToken(authToken.replaceFirst("Bearer ", ""));
                }

                // Since by this time KeycloakAuthenticationProcessingFilter has already finished
                // authenticating our bearer token, the Security context already has keycloak's
                // Authenticated user, to enable this user to work within fineract context,
                // we need to convert keycloak user to custom fineract user in the context
                Authentication keyCloakAuthenticatedObject = SecurityContextHolder.getContext().getAuthentication();

                // load this user from fineract database to be sure they exist
                UserDetails userDetails = null;
                try {
                    Object principal = keyCloakAuthenticatedObject.getPrincipal();
                    String username = "";
                    if (principal instanceof AppUser) {
                        username = ((AppUser) principal).getUsername();
                    } else if (principal instanceof Jwt) {
                        username = (String) (((Jwt) principal).getClaims().get("email"));
                    }
                    if (!username.endsWith("@oneacrefund.org")) {
                        LOG.error("User not recognized: {}", username);
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "User not authorized to access this resource");
                        return;
                    }
                    userDetails = userDetailsService.loadUserByUsername(username);
                    if (userDetails != null) {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
                                keycloakUserCreationService.resolveAuthoritiesFromUserDetails(userDetails).getLeft());
                        // update user authorities if they're changed
                        SecurityContextHolder.clearContext();
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }

                } catch (UsernameNotFoundException ex) {
                    LOG.error("Keycloak user not found in database: {}", ex.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                         "User authenticated by Keycloak but not found");
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (final InvalidTenantIdentifierException e) {
            SecurityContextHolder.getContext().setAuthentication(null);
            ThreadLocalContextUtil.clear();
            LOG.error("Invalid tenant identifier exception occurred: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant identifier.");
        } finally {
            task.stop();
            final PlatformRequestLog log = PlatformRequestLog.from(task, request);
            LOG.debug("{}", log.toString());// check on this
            ThreadLocalContextUtil.clear();
        }
    }

}
