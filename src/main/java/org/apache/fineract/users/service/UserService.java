package org.apache.fineract.users.service;

import org.apache.fineract.organisation.user.UserPermissionsDto;

import javax.servlet.http.HttpServletResponse;

public interface UserService {

    /**
     * Retrieve users by their id
     * @param id
     * @param response
     * @return
     */
    UserPermissionsDto retrieveUserById(Long id, HttpServletResponse response);

    /**
     * Retrieve users by their username
     * @param username
     * @param response
     * @return
     */
    UserPermissionsDto retrieveUserByUsername(String username, HttpServletResponse response);
}
