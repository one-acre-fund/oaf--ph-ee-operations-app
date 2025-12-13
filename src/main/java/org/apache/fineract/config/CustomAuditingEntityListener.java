package org.apache.fineract.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.events.NewAuditEvent;
import org.apache.fineract.audit.service.AuditService;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.parent.AbstractPersistableCustom;
import org.apache.fineract.organisation.user.AppUser;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.PostPersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;


@Component
@Slf4j
public class CustomAuditingEntityListener extends AuditingEntityListener {

    @PostPersist
    public void afterPersist(Object entity) {
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
            AppUser maker = getCurrentUser();
            NewAuditEvent event = new NewAuditEvent(this, getEntityId(entity), action, entity.getClass().getSimpleName(), null, getObjectString(entity), maker, "SUCCESS", LocalDateTime.now());
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
        return ThreadLocalContextUtil.getCurrentUser();
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

