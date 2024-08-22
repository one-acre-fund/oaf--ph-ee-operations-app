package org.apache.fineract.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.events.NewAuditEvent;
import org.apache.fineract.audit.service.AuditService;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.organisation.user.AppUserRepository;
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
    private void logAction(String action, Object entity) {
        if(entity instanceof AuditSource) {
            return;
        }
        NewAuditEvent event = new NewAuditEvent(this, getEntityId(entity), action, entity.getClass().getSimpleName(), null, getObjectString(entity), getCurrentUser(), "SUCCESS", LocalDateTime.now());
        AuditService auditService = BeanUtil.getBean(AuditService.class);
        auditService.createNewEntry(event);
    }

    private Long getEntityId(Object entity) {
        if (entity instanceof AbstractPersistableCustom) {
            return ((AbstractPersistableCustom<?>) entity).getId();
        }
        return null;
    }

    private AppUser getCurrentUser() {
        AppUserRepository appUserRepository = getBean(AppUserRepository.class);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? appUserRepository.findAppUserByName(authentication.getName()): null;
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

