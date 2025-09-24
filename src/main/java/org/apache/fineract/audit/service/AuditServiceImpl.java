package org.apache.fineract.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.data.AuditSourceRepository;
import org.apache.fineract.audit.data.AuditTemplateResponse;
import org.apache.fineract.audit.events.NewAuditEvent;
import org.apache.fineract.organisation.permission.PermissionRepository;
import org.apache.fineract.organisation.user.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@Slf4j
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditSourceRepository auditSourceRepository;
    private final AppUserRepository appUserRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditSource createNewEntry(NewAuditEvent event) {
        AuditSource audit = new AuditSource();
        audit.setEntityName(event.getEntityName());
        audit.setResourceId(event.getResourceId());
        audit.setActionName(event.getActionName());
        audit.setDataAsJson(event.getDataAsJson());
        audit.setProcessingResult(event.getProcessingResult());
        audit.setMadeOnDate(event.getMadeOnDate());
        audit.setMaker(event.getMaker());
        return auditSourceRepository.save(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditSource> getAudits(Specification<AuditSource> specification, Pageable pageable) {
        return auditSourceRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditTemplateResponse retrieveAuditTemplate() {
        return new AuditTemplateResponse(appUserRepository.findAllUsers(), permissionRepository.findDistinctActionNames(), permissionRepository.findDistinctEntityName());
    }

    @Override
    @Transactional(readOnly = true)
    public AuditSource findById(Long id) {
        return auditSourceRepository.findById(id).orElse(null);
    }
}
