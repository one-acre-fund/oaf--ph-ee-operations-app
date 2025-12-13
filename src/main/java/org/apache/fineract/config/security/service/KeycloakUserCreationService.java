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
package org.apache.fineract.config.security.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for synchronize user details between Keycloak and Fineract system.
 */
@Slf4j
@Service
public class KeycloakUserCreationService {
    public static final Integer KEYCLOAK_RANDOM_PWD_CHARACTERS = 16;

    @Autowired
    private AppUserRepository appuserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Pair<Collection<GrantedAuthority>, Set<String>> resolveAuthoritiesFromUserDetails(UserDetails userDetails) {
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        final Set<String> permissionNames = new HashSet<>();
        for (final Role role : ((AppUser) userDetails).getRoles()) {
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getCode()));
                permissionNames.add(permission.getCode());
            }
        }
        return Pair.of(authorities, permissionNames);
    }

    public AppUser createUserFromKeycloakUserData(Authentication token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof Jwt)) {
            return null;
        }
        Jwt principal = (Jwt) principalObj;
        String username = null;
            if (principal.getClaims() != null) {
                username = principal.getClaims().get("preferred_username").toString();
                String password = new RandomPasswordGenerator(KEYCLOAK_RANDOM_PWD_CHARACTERS).generate();
                AppUser appUser = getAppUser(principal.getClaims(), username, passwordEncoder.encode(password));
                return appuserRepository.saveAndFlush(appUser);
            }
            return null;
    }

    private AppUser getAppUser(Map<String, Object> accessToken, String username, String password) {
        AppUser appUser = new AppUser();
        appUser.setUsername(username);
        appUser.setPassword(passwordEncoder.encode(password));
        appUser.setEmail(accessToken.get("email").toString());
        appUser.setFirstname(accessToken.get("given_name").toString());
        appUser.setLastname(accessToken.get("family_name").toString());
        appUser.setFirstTimeLoginRemaining(false);
        appUser.setAccountNonExpired(true);
        appUser.setAccountNonLocked(true);
        appUser.setEnabled(true);
        appUser.setCredentialsNonExpired(true);
        appUser.setDeleted(false);
        appUser.setLastTimePasswordUpdated(new Date());
        appUser.setCreatedDate(LocalDateTime.now());
        appUser.setLastModifiedDate(LocalDateTime.now());
        appUser.setLastTimePasswordUpdated(new Date());
        appUser.setLastTimePasswordUpdated(new Date());
        appUser.setLastTimePasswordUpdated(new Date());
        return appUser;
    }
}
