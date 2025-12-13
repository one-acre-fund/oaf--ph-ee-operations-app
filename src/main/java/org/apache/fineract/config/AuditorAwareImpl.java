package org.apache.fineract.config;

import lombok.NonNull;
import org.apache.fineract.config.security.utils.SecurityUtils;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.user.AppUser;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        AppUser currentAppUser = ThreadLocalContextUtil.getCurrentUser();
        if (currentAppUser != null) {
            return Optional.ofNullable(currentAppUser.getUsername());
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return Optional.of(SecurityUtils.extractUsername(authentication));
        }
        return Optional.empty();
    }
}

