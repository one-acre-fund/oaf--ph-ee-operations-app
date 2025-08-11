package org.apache.fineract.test;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.fineract.config.security.service.KeycloakUserCreationService;
import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakUserCreationServiceTest {

    private KeycloakUserCreationService service;

    @BeforeEach
    void setUp() {
        service = new KeycloakUserCreationService();
    }

    @Test
    @DisplayName("Should resolve authorities and permission names from user details")
    void resolveAuthoritiesFromUserDetails() {
        Permission perm1 = new Permission();
        perm1.setCode("READ_CLIENT");

        Permission perm2 = new Permission();
        perm2.setCode("WRITE_CLIENT");

        Permission perm3 = new Permission();
        perm3.setCode("DELETE_CLIENT");

        Role role1 = new Role();
        Set<Permission> perms1 = new HashSet<>();
        perms1.add(perm1);
        perms1.add(perm2);
        role1.setPermissions(perms1);

        Role role2 = new Role();
        Set<Permission> perms2 = new HashSet<>();
        perms2.add(perm3);
        role2.setPermissions(perms2);

        AppUser user = new AppUser();
        List<Role> roles = new ArrayList<>();
        roles.add(role1);
        roles.add(role2);
        user.setRoles(roles);

        Pair<Collection<GrantedAuthority>, Set<String>> result = service.resolveAuthoritiesFromUserDetails(user);

        assertNotNull(result);
        assertEquals(3, result.getLeft().size());
        assertEquals(3, result.getRight().size());

        List<String> authorityList = new ArrayList<>();
        for (GrantedAuthority auth : result.getLeft()) {
            authorityList.add(auth.getAuthority());
        }

        assertTrue(authorityList.contains("READ_CLIENT"));
        assertTrue(authorityList.contains("WRITE_CLIENT"));
        assertTrue(authorityList.contains("DELETE_CLIENT"));

        assertTrue(result.getRight().contains("READ_CLIENT"));
        assertTrue(result.getRight().contains("WRITE_CLIENT"));
        assertTrue(result.getRight().contains("DELETE_CLIENT"));
    }

    @Test
    @DisplayName("Should return empty authorities and permissions when user has no roles")
    void resolveAuthoritiesWithNoRoles() {
        AppUser user = new AppUser();
        user.setRoles(Collections.emptyList());

        Pair<Collection<GrantedAuthority>, Set<String>> result = service.resolveAuthoritiesFromUserDetails(user);

        assertNotNull(result);
        assertTrue(result.getLeft().isEmpty());
        assertTrue(result.getRight().isEmpty());
    }

    @Test
    @DisplayName("Should handle roles with empty permission sets")
    void resolveAuthoritiesWithEmptyPermissions() {
        Role emptyPermRole = new Role();
        emptyPermRole.setPermissions(Collections.<Permission>emptySet());

        AppUser user = new AppUser();
        List<Role> roles = new ArrayList<>();
        roles.add(emptyPermRole);
        user.setRoles(roles);

        Pair<Collection<GrantedAuthority>, Set<String>> result = service.resolveAuthoritiesFromUserDetails(user);

        assertNotNull(result);
        assertTrue(result.getLeft().isEmpty());
        assertTrue(result.getRight().isEmpty());
    }
}

