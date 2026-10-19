package org.apache.fineract.export.read;

import lombok.Builder;
import lombok.Getter;
import org.apache.fineract.operations.Filter;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Validated filters of one export. Validation (and the 400 response) happens before a job is created.
 */
@Getter
@Builder
public class ExportCriteria {

    /** Inclusive lower bound on STARTED_AT. */
    private final Date startFrom;

    /** Inclusive upper bound on STARTED_AT. */
    private final Date startTo;

    @Builder.Default
    private final Map<Filter, List<String>> filters = Collections.emptyMap();

    @Builder.Default
    private final DataScope scope = DataScope.UNRESTRICTED;

    @Builder.Default
    private final Sort.Direction direction = Sort.Direction.DESC;

    public boolean hasDateRange() {
        return startFrom != null || startTo != null;
    }
}
