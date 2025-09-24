package org.apache.fineract.audit.events;

import lombok.Getter;
import org.apache.fineract.organisation.user.AppUser;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Audit event data class mapping data for an async audit event.
 */
@Getter
public class NewAuditEvent extends ApplicationEvent {
    private final Long resourceId;

    private final String actionName;

    private final String entityName;

    private final String resourceGetUrl;

    private final String dataAsJson;

    private final AppUser maker;

    private final String processingResult;

    private LocalDateTime madeOnDate;

    public NewAuditEvent(Object source, Long resourceId, String actionName, String entityName, String resourceGetUrl, String dataAsJson, AppUser maker, String processingResult,LocalDateTime madeOnDate) {
        super(source);
        this.resourceId = resourceId;
        this.actionName = actionName;
        this.entityName = entityName;
        this.resourceGetUrl = resourceGetUrl;
        this.dataAsJson = dataAsJson;
        this.maker = maker;
        this.processingResult = processingResult;
        this.madeOnDate = madeOnDate;
    }
}
