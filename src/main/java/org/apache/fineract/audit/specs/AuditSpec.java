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
import javax.persistence.criteria.Predicate;

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

            LocalDateTime startDateTime = request.getMakerDateTimeFrom() != null
                    ? request.getMakerDateTimeFrom().withHour(0).withMinute(0).withSecond(0).withNano(0)
                    : null;
            LocalDateTime endDateTime = request.getMakerDateTimeTo() != null
                    ? request.getMakerDateTimeTo().withHour(23).withMinute(59).withSecond(59).withNano(999999999)
                    : null;
            // If both are missing, no date filter is applied
            if (startDateTime != null || endDateTime != null) {
                if (startDateTime == null) {
                    startDateTime = LocalDateTime.MIN;
                }
                if (endDateTime == null) {
                    endDateTime = LocalDateTime.now();
                }
                predicates.add(cb.between(root.get("madeOnDate"), startDateTime, endDateTime));
            }

            // Additional filters
            if (request.getActionName() != null) {
                predicates.add(cb.like(cb.lower(root.get("actionName")), "%" + request.getActionName().toLowerCase() + "%"));
            }
            if (request.getEntityName() != null) {
                predicates.add(cb.like(cb.lower(root.get("entityName")), "%" + request.getEntityName().toLowerCase() + "%"));
            }
            if (request.getProcessingResult() != null) {
                predicates.add(cb.like(cb.lower(root.get("processingResult")), "%" + request.getProcessingResult().toLowerCase() + "%"));
            }
            if (request.getMakerId() != null) {
                predicates.add(cb.equal(root.get("maker"), request.getMakerId()));
            }
            if (request.getResourceId() != null) {
                predicates.add(cb.equal(root.get("resourceId"), request.getResourceId()));

            }

            // Combine all predicates
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    /**
     * Gets a date adjusted to a specific time of day.
     *
     * @param date The input date.
     * @param hour The target hour.
     * @param minute The target minute.
     * @return The adjusted date with the specified time of day.
     */

    private Date getExactDate(Date date, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
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
