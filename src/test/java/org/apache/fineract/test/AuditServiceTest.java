package org.apache.fineract.test;

import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.data.AuditSourceRepository;
import org.apache.fineract.audit.events.NewAuditEvent;
import org.apache.fineract.audit.service.AuditServiceImpl;
import org.apache.fineract.organisation.user.AppUser;
import org.apache.fineract.utils.DateUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditServiceTest {
    private AuditSourceRepository auditSourceRepository;
    private AuditServiceImpl auditService;
    private DateUtil dateUtil;

    @BeforeEach
    void setUp() {
        auditSourceRepository = mock(AuditSourceRepository.class);
        auditService = new AuditServiceImpl(auditSourceRepository, null, null);
        dateUtil = new DateUtil();
    }

    @Test
    void testCreateNewEntry() {
        // Given
        LocalDateTime madeOn = dateUtil.getLocalDateTimeOfTenant(null);
        NewAuditEvent event = new NewAuditEvent(this, 1L, "CREATE", "AppUser", "", "data", mock(AppUser.class), "SUCCESS", madeOn);
        AuditSource savedAudit = new AuditSource();
        when(auditSourceRepository.save(any(AuditSource.class))).thenReturn(savedAudit);

        // When
        AuditSource result = auditService.createNewEntry(event);

        // Then
        Assertions.assertEquals(result, savedAudit);
        verify(auditSourceRepository).save(any(AuditSource.class));
    }

    @Test
    void testGetAudits() {
        // Given
        Specification<AuditSource> specification = mock(Specification.class);
        Pageable pageable = mock(Pageable.class);
        Page<AuditSource> auditPage = mock(Page.class);
        when(auditSourceRepository.findAll(specification, pageable)).thenReturn(auditPage);

        // When
        Page<AuditSource> result = auditService.getAudits(specification, pageable);

        // Then
        Assertions.assertEquals(result, auditPage);
        verify(auditSourceRepository).findAll(specification, pageable);
    }
}
