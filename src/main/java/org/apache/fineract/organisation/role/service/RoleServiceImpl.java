package org.apache.fineract.organisation.role.service;

import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.role.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Optional;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void createRole(Role role, HttpServletResponse response) {
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

    @Override
    public void updateRole(Long roleId, Role role, HttpServletResponse response) {
        Optional<Role> optionalExisting = roleRepository.findById(roleId);
        if (optionalExisting.isPresent()) {
            Role existing = optionalExisting.get();
            String normalizedName = normalizeRoleName(role.getName());
            role.setName(normalizedName);

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
