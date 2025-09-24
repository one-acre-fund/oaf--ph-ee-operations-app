package org.apache.fineract.audit.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Data class containing search criteria for audit entries.
 */
@Getter
@AllArgsConstructor
public class AuditSearch {
    private final String actionName;
    private final String entityName;
    private final Long resourceId;
    private final Long makerId;
    private final LocalDateTime makerDateTimeFrom;
    private final LocalDateTime makerDateTimeTo;
    private final String processingResult;
}
