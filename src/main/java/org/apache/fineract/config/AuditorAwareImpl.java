package org.apache.fineract.config;

import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        // Return the current logged-in username or a system account if none is available
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}

