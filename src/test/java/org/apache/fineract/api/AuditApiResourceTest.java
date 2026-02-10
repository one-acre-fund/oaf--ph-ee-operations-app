package org.apache.fineract.api;

import org.apache.fineract.audit.data.AuditTemplateResponse;
import org.apache.fineract.audit.service.AuditService;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.core.service.ThreadLocalContextUtil;
import org.apache.fineract.organisation.user.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditApiResourceTest {

    @Mock
    private AuditService auditService;

    @Mock
    private AppUser connectedUser;

    @InjectMocks
    private AuditApiResource auditApiResource;

    private MockedStatic<ThreadLocalContextUtil> mockedThreadLocalContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockedThreadLocalContext = mockStatic(ThreadLocalContextUtil.class);
        mockedThreadLocalContext.when(ThreadLocalContextUtil::getCurrentUser).thenReturn(connectedUser);
    }

    @AfterEach
    void tearDown() {
        if (mockedThreadLocalContext != null) {
            mockedThreadLocalContext.close();
        }
    }

    @DisplayName("Retrieve audit entries with all parameters")
    @Test
    void test_retrieve_audit_entries_with_all_parameters() {
        // Arrange
        String actionName = "UPDATE";
        String entityName = "AppUser";
        Long resourceId = 123L;
        Long makerId = 456L;
        String makerDateTimeFrom = "2024-01-01T00:00:00";
        String makerDateTimeTo = "2024-12-31T23:59:59";
        String processingResult = "SUCCESS";
        Integer page = 0;
        Integer limit = 10;
        String orderBy = "id";
        String sortOrder = "DESC";
        String dateFormat = null;

        AuditSource audit1 = new AuditSource();
        AuditSource audit2 = new AuditSource();
        List<AuditSource> auditList = Arrays.asList(audit1, audit2);
        Page<AuditSource> auditPage = new PageImpl<>(auditList);

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.getAudits(any(Specification.class), any(PageRequest.class))).thenReturn(auditPage);

        // Act
        Page<AuditSource> result = auditApiResource.retrieveAuditEntries(
                actionName, entityName, resourceId, makerId,
                makerDateTimeFrom, makerDateTimeTo, processingResult,
                page, limit, orderBy, sortOrder, dateFormat
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(connectedUser).validateHasReadPermission("AUDIT");
        verify(auditService).getAudits(any(Specification.class), any(PageRequest.class));
    }

    @DisplayName("Retrieve audit entries with default pagination parameters")
    @Test
    void test_retrieve_audit_entries_with_default_pagination() {
        // Arrange
        AuditSource audit = new AuditSource();
        Page<AuditSource> auditPage = new PageImpl<>(Collections.singletonList(audit));

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.getAudits(any(Specification.class), any(PageRequest.class))).thenReturn(auditPage);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);

        // Act
        Page<AuditSource> result = auditApiResource.retrieveAuditEntries(
                null, null, null, null, null, null, null,
                null, null, null, null, null
        );

        // Assert
        assertNotNull(result);
        verify(auditService).getAudits(any(Specification.class), pageRequestCaptor.capture());

        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertEquals(0, capturedPageRequest.getPageNumber());
        assertEquals(10, capturedPageRequest.getPageSize());
        assertEquals(Sort.Direction.ASC, capturedPageRequest.getSort().getOrderFor("id").getDirection());
    }

    @DisplayName("Retrieve audit entries with custom page and limit")
    @Test
    void test_retrieve_audit_entries_with_custom_page_and_limit() {
        // Arrange
        Integer page = 2;
        Integer limit = 20;

        Page<AuditSource> auditPage = new PageImpl<>(Collections.emptyList());

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.getAudits(any(Specification.class), any(PageRequest.class))).thenReturn(auditPage);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);

        // Act
        auditApiResource.retrieveAuditEntries(
                null, null, null, null, null, null, null,
                page, limit, null, null, null
        );

        // Assert
        verify(auditService).getAudits(any(Specification.class), pageRequestCaptor.capture());

        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertEquals(2, capturedPageRequest.getPageNumber());
        assertEquals(20, capturedPageRequest.getPageSize());
    }

    @DisplayName("Retrieve audit entries with custom sort order")
    @Test
    void test_retrieve_audit_entries_with_custom_sort() {
        // Arrange
        String orderBy = "createdDate";
        String sortOrder = "DESC";

        Page<AuditSource> auditPage = new PageImpl<>(Collections.emptyList());

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.getAudits(any(Specification.class), any(PageRequest.class))).thenReturn(auditPage);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);

        // Act
        auditApiResource.retrieveAuditEntries(
                null, null, null, null, null, null, null,
                null, null, orderBy, sortOrder, null
        );

        // Assert
        verify(auditService).getAudits(any(Specification.class), pageRequestCaptor.capture());

        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertEquals(Sort.Direction.DESC, capturedPageRequest.getSort().getOrderFor("createdDate").getDirection());
    }

    @DisplayName("Retrieve audit entries with custom date format")
    @Test
    void test_retrieve_audit_entries_with_custom_date_format() {
        // Arrange
        String makerDateTimeFrom = "01-01-2024 00:00:00";
        String makerDateTimeTo = "31-12-2024 23:59:59";
        String dateFormat = "dd-MM-yyyy HH:mm:ss";

        Page<AuditSource> auditPage = new PageImpl<>(Collections.emptyList());

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.getAudits(any(Specification.class), any(PageRequest.class))).thenReturn(auditPage);

        // Act
        Page<AuditSource> result = auditApiResource.retrieveAuditEntries(
                null, null, null, null,
                makerDateTimeFrom, makerDateTimeTo, null,
                null, null, null, null, dateFormat
        );

        // Assert
        assertNotNull(result);
        verify(auditService).getAudits(any(Specification.class), any(PageRequest.class));
    }

    @DisplayName("Retrieve audit entries filters by action name")
    @Test
    void test_retrieve_audit_entries_filter_by_action_name() {
        // Arrange
        String actionName = "CREATE";
        Page<AuditSource> auditPage = new PageImpl<>(Collections.emptyList());

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.getAudits(any(Specification.class), any(PageRequest.class))).thenReturn(auditPage);

        // Act
        Page<AuditSource> result = auditApiResource.retrieveAuditEntries(
                actionName, null, null, null, null, null, null,
                null, null, null, null, null
        );

        // Assert
        assertNotNull(result);
        verify(connectedUser).validateHasReadPermission("AUDIT");
    }

    @DisplayName("Retrieve audit template successfully")
    @Test
    void test_retrieve_audit_template_successfully() {
        // Arrange
        AuditTemplateResponse templateResponse = new AuditTemplateResponse();

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.retrieveAuditTemplate()).thenReturn(templateResponse);

        // Act
        AuditTemplateResponse result = auditApiResource.retrieveAuditTemplate();

        // Assert
        assertNotNull(result);
        assertEquals(templateResponse, result);
        verify(connectedUser).validateHasReadPermission("AUDIT");
        verify(auditService).retrieveAuditTemplate();
    }

    @DisplayName("Get audit by id successfully")
    @Test
    void test_get_audit_by_id_successfully() {
        // Arrange
        Long auditId = 100L;
        AuditSource audit = new AuditSource();

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.findById(auditId)).thenReturn(audit);

        // Act
        AuditSource result = auditApiResource.getAudit(auditId);

        // Assert
        assertNotNull(result);
        assertEquals(audit, result);
        verify(connectedUser).validateHasReadPermission("AUDIT");
        verify(auditService).findById(auditId);
    }

    @DisplayName("Get audit by id returns null when not found")
    @Test
    void test_get_audit_by_id_not_found() {
        // Arrange
        Long auditId = 999L;

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.findById(auditId)).thenReturn(null);

        // Act
        AuditSource result = auditApiResource.getAudit(auditId);

        // Assert
        assertNull(result);
        verify(connectedUser).validateHasReadPermission("AUDIT");
        verify(auditService).findById(auditId);
    }

    @DisplayName("Retrieve audit entries with empty result")
    @Test
    void test_retrieve_audit_entries_empty_result() {
        // Arrange
        Page<AuditSource> emptyPage = new PageImpl<>(Collections.emptyList());

        doNothing().when(connectedUser).validateHasReadPermission("AUDIT");
        when(auditService.getAudits(any(Specification.class), any(PageRequest.class))).thenReturn(emptyPage);

        // Act
        Page<AuditSource> result = auditApiResource.retrieveAuditEntries(
                "DELETE", "NonExistentEntity", null, null, null, null, null,
                null, null, null, null, null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        verify(auditService).getAudits(any(Specification.class), any(PageRequest.class));
    }
}
