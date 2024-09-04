package org.apache.fineract.audit.specs;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.audit.data.AuditSearch;
import org.apache.fineract.audit.data.AuditSource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.springframework.data.jpa.domain.Specification.where;
/**
 * A component that generates specifications for querying audit entries based on provided search criteria.
 */
@Component
@Slf4j
public class AuditSpec extends BaseSpecification<AuditSource, AuditSearch> {
    /**
     * Generates a specification for filtering audit entries based on the provided search criteria.
     *
     * @param request The audit search criteria.
     * @return A specification for filtering audit entries.
     */
    @Override
    public Specification<AuditSource> getFilter(AuditSearch request) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            addDateFilterPredicates(request, root, cb, predicates);
            addActionNameFilterPredicate(request, root, cb, predicates);
            addEntityNameFilterPredicate(request, root, cb, predicates);
            addProcessingResultFilterPredicate(request, root, cb, predicates);
            addMakerIdFilterPredicate(request, root, cb, predicates);
            addResourceIdFilterPredicate(request, root, cb, predicates);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void addDateFilterPredicates(AuditSearch request, Root<AuditSource> root, CriteriaBuilder cb, List<Predicate> predicates) {
        LocalDateTime startDateTime = request.getMakerDateTimeFrom() != null
                ? request.getMakerDateTimeFrom().withHour(0).withMinute(0).withSecond(0).withNano(0)
                : null;
        LocalDateTime endDateTime = request.getMakerDateTimeTo() != null
                ? request.getMakerDateTimeTo().withHour(23).withMinute(59).withSecond(59).withNano(999999999)
                : null;
        if (startDateTime != null || endDateTime != null) {
            if (startDateTime == null) {
                startDateTime = LocalDateTime.MIN;
            }
            if (endDateTime == null) {
                endDateTime = LocalDateTime.now();
            }
            predicates.add(cb.between(root.get("madeOnDate"), startDateTime, endDateTime));
        }
    }

    private void addActionNameFilterPredicate(AuditSearch request, Root<AuditSource> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (request.getActionName() != null) {
            predicates.add(cb.like(cb.lower(root.get("actionName")), "%" + request.getActionName().toLowerCase() + "%"));
        }
    }

    private void addEntityNameFilterPredicate(AuditSearch request, Root<AuditSource> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (request.getEntityName() != null) {
            predicates.add(cb.like(cb.lower(root.get("entityName")), "%" + request.getEntityName().toLowerCase() + "%"));
        }
    }

    private void addProcessingResultFilterPredicate(AuditSearch request, Root<AuditSource> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (request.getProcessingResult() != null) {
            predicates.add(cb.like(cb.lower(root.get("processingResult")), "%" + request.getProcessingResult().toLowerCase() + "%"));
        }
    }

    private void addMakerIdFilterPredicate(AuditSearch request, Root<AuditSource> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (request.getMakerId() != null) {
            predicates.add(cb.equal(root.get("maker"), request.getMakerId()));
        }
    }

    private void addResourceIdFilterPredicate(AuditSearch request, Root<AuditSource> root, CriteriaBuilder cb, List<Predicate> predicates) {
        if (request.getResourceId() != null) {
            predicates.add(cb.equal(root.get("resourceId"), request.getResourceId()));
        }
    }

    /**
     * Generates a specification for filtering audit entries based on the provided attribute and string value.
     *
     * @param attribute The attribute to filter on.
     * @param value The string value to match.
     * @return A specification for filtering audit entries based on the string attribute and value.
     */
    private Specification<AuditSource> fieldContains(String attribute, String value) {
        return (root, query, cb) -> {
            if (value == null) {
                return null;
            }
            return cb.like(
                    cb.lower(root.get(attribute)),
                    containsLowerCase(value)
            );
        };
    }
    /**
     * Generates a specification for filtering audit entries based on the provided attribute and long value.
     *
     * @param attribute The attribute to filter on.
     * @param value The long value to match.
     * @return A specification for filtering audit entries based on the long attribute and value.
     */
    private Specification<AuditSource> fieldContains(String attribute, Long value) {
        return (root, query, cb) -> {
            if (value == null) {
                return null;
            }
            return cb.equal(root.get(attribute), value);
        };
    }

    /**
     * Generates a specification for filtering audit entries based on the provided date range.
     *
     * @param field The field to filter on.
     * @param start The start date of the date range.
     * @param end The end date of the date range.
     * @return A specification for filtering audit entries within the specified date range.
     */
    private Specification<AuditSource> dateBetween(String field, Date start, Date end) {
        return (root, query, cb)
                -> {
            if (start == null) {
                return null;
            }
            return cb.between(root.get(field), start, end);
        };
    }
}
