package org.apache.fineract.users.service;

import org.apache.fineract.organisation.user.AppUserDto;
import org.apache.fineract.organisation.user.AppUserUpdateDto;

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

    /**
     * Update user with partial data
     * @param userId the user ID to update
     * @param updateDto the partial update data
     * @param response the HTTP response
     * @return true if user was updated, false if not found
     */
    boolean updateUser(Long userId, AppUserUpdateDto updateDto, HttpServletResponse response);
}
