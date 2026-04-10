package org.apache.fineract.users.service;

import org.apache.fineract.organisation.user.AppUserDto;

import javax.servlet.http.HttpServletResponse;

public interface UserService {

    /**
     * Retrieve users by their id
     * @param id
     * @param response
     * @return
     */
    AppUserDto retrieveUserById(Long id, HttpServletResponse response);

    /**
     * Retrieve users by their username
     * @param username
     * @param response
     * @return
     */
    AppUserDto retrieveUserByUsername(String username, HttpServletResponse response);
}
