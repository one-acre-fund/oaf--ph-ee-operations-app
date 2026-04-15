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
package org.apache.fineract.api;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.permission.PermissionData;
import org.apache.fineract.organisation.permission.PermissionRepository;
import org.apache.fineract.organisation.role.PermissionsCommand;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RolePermissionsData;
import org.apache.fineract.organisation.role.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.apache.fineract.api.AssignmentAction.ASSIGN;

@RestController
@SecurityRequirement(name = "BearerAuth")
public class RolesApi {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private final String resourceNameForPermissions = "ROLE";

    @GetMapping(path = "/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Role> retrieveAll() {
        ThreadLocalContextUtil.getCurrentUser().validateHasReadPermission(this.resourceNameForPermissions);
        return this.roleRepository.findAll();
    }

    @GetMapping(path = "/role/{roleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Role retrieveOne(@PathVariable("roleId") Long roleId, HttpServletResponse response) {
        ThreadLocalContextUtil.getCurrentUser().validateHasReadPermission(this.resourceNameForPermissions);
        Optional<Role> optionalRole = roleRepository.findById(roleId);
        if(optionalRole.isPresent()) {
            return optionalRole.get();
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    @GetMapping(path = "/role/{roleId}/permissions", produces = MediaType.APPLICATION_JSON_VALUE)
    public RolePermissionsData retrievePermissions(@PathVariable("roleId") Long roleId, HttpServletResponse response) {
        ThreadLocalContextUtil.getCurrentUser().validateHasReadPermission(this.resourceNameForPermissions);
        Optional<Role> optionalRole = roleRepository.findById(roleId);
        if(optionalRole.isPresent()) {
            Role role = optionalRole.get();
            final List<PermissionData> permissionUsageData = this.permissionRepository.findAllPermissionsWithRoleSelection(roleId);
            return new RolePermissionsData(role.getId(), role.getName(), role.getDescription(), role.getDisabled(), permissionUsageData);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    @PostMapping(path = "/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void create(@RequestBody Role role, HttpServletResponse response) {
        ThreadLocalContextUtil.getCurrentUser().validateHasCreatePermission(this.resourceNameForPermissions);
        
        // Normalize role name: lowercase with no spaces
        String normalizedName = normalizeRoleName(role.getName());
        role.setName(normalizedName);
        
        Role existing = roleRepository.getRoleByName(normalizedName);
        if (existing == null) {
            role.setId(null);
            roleRepository.saveAndFlush(role);
        } else {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
        }
    }

    @PutMapping(path = "/role/{roleId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void update(@PathVariable("roleId") Long roleId, @RequestBody Role role, HttpServletResponse response) {
        ThreadLocalContextUtil.getCurrentUser().validateHasUpdatePermission(this.resourceNameForPermissions);
        Optional<Role> optionalExisting = roleRepository.findById(roleId);
        if (optionalExisting.isPresent()) {
            Role existing = optionalExisting.get();
            
            // Normalize role name: lowercase with no spaces
            String normalizedName = normalizeRoleName(role.getName());
            role.setName(normalizedName);
            
            // Check if the name is being changed and if the new name already exists for a different role
            if (!existing.getName().equals(normalizedName)) {
                Role roleWithSameName = roleRepository.getRoleByName(normalizedName);
                if (roleWithSameName != null && !Objects.equals(roleWithSameName.getId(), roleId)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
            
            role.setId(roleId);
            role.setAppUsers(existing.getAppusers());
            role.setPermissions(existing.getPermissions());
            roleRepository.saveAndFlush(role);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }


    @DeleteMapping(path = "/role/{roleId}")
    public void delete(@PathVariable("roleId") Long roleId, HttpServletResponse response) {
        ThreadLocalContextUtil.getCurrentUser().validateHasDeletePermission(this.resourceNameForPermissions);
        Optional<Role> optionalRole = roleRepository.findById(roleId);
        if(optionalRole.isPresent()) {
            Role role = optionalRole.get();
            // Clear relationships before deleting to avoid orphan removal issues
            role.getPermissions().clear();
            role.getAppusers().clear();
            roleRepository.saveAndFlush(role);
            roleRepository.deleteById(roleId);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @PostMapping(path = "/role/{roleId}")
    public void updateRoleStatus(@PathVariable("roleId") Long roleId, 
                                  @RequestParam("command") String command,
                                  HttpServletResponse response) {
        Optional<Role> optionalRole = roleRepository.findById(roleId);
        if (optionalRole.isPresent()) {
            Role role = optionalRole.get();
            if ("disable".equalsIgnoreCase(command)) {
                ThreadLocalContextUtil.getCurrentUser().validateHasPermission("DISABLE", this.resourceNameForPermissions);
                role.setDisabled(true);
                roleRepository.saveAndFlush(role);
            } else if ("enable".equalsIgnoreCase(command)) {
                ThreadLocalContextUtil.getCurrentUser().validateHasPermission("ENABLE", this.resourceNameForPermissions);
                role.setDisabled(false);
                roleRepository.saveAndFlush(role);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @PutMapping(path = "/role/{roleId}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void permissionAssignment(@PathVariable("roleId") Long roleId, @RequestBody PermissionsCommand permissionsCommand,
                                     HttpServletResponse response) {
        ThreadLocalContextUtil.getCurrentUser().validateHasUpdatePermission(this.resourceNameForPermissions);
        Optional<Role> optionalRole = roleRepository.findById(roleId);
        if (optionalRole.isPresent()) {
            Role existingRole = optionalRole.get();
            final Collection<Permission> allPermissions = this.permissionRepository.findAll();
            final Map<String, Boolean> commandPermissions = permissionsCommand.getPermissions();
            final Map<String, Boolean> changedPermissions = new HashMap<>();
            
            for (final String permissionCode : commandPermissions.keySet()) {
                final boolean isSelected = commandPermissions.get(permissionCode).booleanValue();
                final Permission permission = findPermissionByCode(allPermissions, permissionCode);
                
                if (permission != null) {
                    boolean changed = false;
                    if (isSelected && !existingRole.getPermissions().contains(permission)) {
                        existingRole.getPermissions().add(permission);
                        changed = true;
                    } else if (!isSelected && existingRole.getPermissions().contains(permission)) {
                        existingRole.getPermissions().remove(permission);
                        changed = true;
                    }
                    
                    if (changed) {
                        changedPermissions.put(permissionCode, isSelected);
                    }
                }
            }
            
            if (!changedPermissions.isEmpty()) {
                this.roleRepository.saveAndFlush(existingRole);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    private Permission findPermissionByCode(final Collection<Permission> permissions, final String permissionCode) {
        for (final Permission permission : permissions) {
            if (permission.getCode().equalsIgnoreCase(permissionCode)) {
                return permission;
            }
        }
        return null;
    }
    
    /**
     * Normalizes role name to be lowercase with no spaces
     * @param roleName the original role name
     * @return normalized role name (lowercase, no spaces)
     */
    private String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return null;
        }
        return roleName.toLowerCase().replaceAll("\\s+", "");
    }
}
