package org.apache.fineract.test;

import org.apache.fineract.audit.data.AuditSearch;
import org.apache.fineract.audit.data.AuditSource;
import org.apache.fineract.audit.specs.AuditSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;

class AuditSpecTest {

    private AuditSpec auditSpec;

    @BeforeEach
    void setUp() {
        auditSpec = new AuditSpec();
    }

    @Test
    void testGetFilter() {
        AuditSearch auditSearch = new AuditSearch("CREATE", "AppUser", 1L, 1L, LocalDateTime.now(), LocalDateTime.now(), "SUCCESS");
        Specification<AuditSource> specification = auditSpec.getFilter(auditSearch);
        Assertions.assertNotNull(specification);
    }

    @Test
    void testGetFilterWithNoSearch() {
        // Given
        AuditSearch auditSearch = new AuditSearch(null, null, null, null, null, null, null);
        Specification<AuditSource> specification = auditSpec.getFilter(auditSearch);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<AuditSource> root = mock(Root.class);
        Predicate predicate = specification.toPredicate(root, query, cb);
        // Then
        Assertions.assertNotNull(specification);
        Assertions.assertNull(predicate);
    }

}
