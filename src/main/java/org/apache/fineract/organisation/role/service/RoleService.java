package org.apache.fineract.organisation.role.service;

import org.apache.fineract.organisation.role.Role;

import javax.servlet.http.HttpServletResponse;

public interface RoleService {

    /**
     * Create a role
     * @param role
     * @param response
     */
    void createRole(Role role, HttpServletResponse response);

    /**
     * Update a role
     * @param roleId
     * @param role
     * @param response
     */
    void updateRole(Long roleId, Role role, HttpServletResponse response);

}
