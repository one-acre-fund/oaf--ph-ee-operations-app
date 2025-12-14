package org.apache.fineract.config.security.utils;

import org.apache.fineract.organisation.user.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public static String extractUsername(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof AppUser) {
            return ((AppUser) principal).getUsername();
        } else if (principal instanceof Jwt) {
            Object claim = ((Jwt) principal).getClaims().get("preferred_username");
            return claim != null ? (String) claim : "";
        }
        return "";
    }
}
