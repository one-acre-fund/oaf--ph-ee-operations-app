package org.apache.fineract.users.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.organisation.permission.Permission;
import org.apache.fineract.organisation.role.Role;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.apache.fineract.organisation.user.AppUserDto;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final AppUserRepository appUserRepository;

    public UserServiceImpl(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AppUserDto retrieveUserById(Long id, HttpServletResponse response) {
        AppUser user = appUserRepository.findById(id).orElse(null);
        return mapToDto(user, response);
    }

    @Override
    public AppUserDto retrieveUserByUsername(String username, HttpServletResponse response) {
        AppUser user = appUserRepository.findAppUserByName(username);
        return mapToDto(user, response);
    }

    public AppUserDto mapToDto(AppUser user, HttpServletResponse response) {
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        Set<String> uniquePermissions = new LinkedHashSet<>();
        Set<String> roles = new LinkedHashSet<>();
        Collection<Role> userRoles = user.getRoles();
        if (userRoles != null) {
            for (Role role : userRoles) {
                if (!role.getDisabled()) {
                    List<String> codes = new ArrayList<>();
                    for (Permission p : role.getPermissions()) {
                        codes.add(p.getCode());
                    }
                    roles.add(role.getName());
                    uniquePermissions.addAll(codes);
                }
            }
        }

        return new AppUserDto(user, uniquePermissions, roles);
    }

}
