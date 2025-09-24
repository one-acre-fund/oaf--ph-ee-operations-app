package org.apache.fineract.audit.service;

import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.data.AuditTemplateResponse;
import org.apache.fineract.audit.events.NewAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
/**
 * A service interface for managing audit-related operations.
 */
public interface AuditService {
    /**
     * Creates a new audit entry based on the provided audit event.
     *
     * @param event The new audit event containing information for the audit entry.
     * @return The newly created audit source entry.
     */
    AuditSource createNewEntry(NewAuditEvent event);
    /**
     * Retrieves a page of audit entries based on the specified criteria.
     *
     * @param specification The specification to filter audit entries.
     * @param pageable      The pagination information for the result page.
     * @return A page of audit entries meeting the specified criteria.
     */
    Page<AuditSource> getAudits(Specification<AuditSource> specification, Pageable pageable);

    /**
     * Retrieves audit templates
     * @return AuditTemplateResponse
     */

    AuditTemplateResponse retrieveAuditTemplate();

    /**
     * Retrieves an audit entry by its unique identifier.
     * @return AuditSource
     */
    AuditSource findById(Long id);
}
