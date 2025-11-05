package org.apache.fineract.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.events.NewAuditEvent;
import org.apache.fineract.audit.service.AuditService;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.keycloak.KeycloakPrincipal;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static org.apache.fineract.config.BeanUtil.getBean;

@Component
@Slf4j
public class CustomAuditingEntityListener extends AuditingEntityListener {

    @PrePersist
    public void onPrePersist(Object entity) {
        logAction("CREATE", entity);
    }

    @PreUpdate
    public void onPreUpdate(Object entity) {
        logAction("UPDATE", entity);
    }

    @PreRemove
    public void onPreRemove(Object entity) {
        logAction("DELETE", entity);
    }

    public void logAction(String action, Object entity) {
        if(entity instanceof AuditSource) {
            return;
        }
        try {
            NewAuditEvent event = new NewAuditEvent(this, getEntityId(entity), action, entity.getClass().getSimpleName(), null, getObjectString(entity), getCurrentUser(), "SUCCESS", LocalDateTime.now());
            AuditService auditService = BeanUtil.getBean(AuditService.class);
            auditService.createNewEntry(event);
        } catch (Exception ex) {
            log.error("Failed to log audit event for action {} on entity {}: {}", action, entity.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private Long getEntityId(Object entity) {
        if (entity instanceof AbstractPersistableCustom) {
            return ((AbstractPersistableCustom<?>) entity).getId();
        }
        return null;
    }

    public AppUser getCurrentUser() {
        AppUserRepository appUserRepository = getBean(AppUserRepository.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof KeycloakPrincipal)) {
            return null;
        }
        KeycloakPrincipal<?> principal = (KeycloakPrincipal<?>) principalObj;
        String username = null;
        if (principal.getKeycloakSecurityContext() != null && principal.getKeycloakSecurityContext().getToken() != null) {
            username = principal.getKeycloakSecurityContext().getToken().getPreferredUsername();
        }
        if (username == null) {
            return null;
        }
        return appUserRepository.findAppUserByName(username);
    }

    private String getObjectString(Object entity) {
        return Optional.ofNullable(entity)
                .map(e -> {
                    try {
                        return Objects.toString(e);
                    } catch (Exception ex) {
                        return null;
                    }
                }).orElse(null);
    }
}

