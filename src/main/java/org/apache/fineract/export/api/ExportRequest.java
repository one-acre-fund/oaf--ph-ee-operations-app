package org.apache.fineract.export.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.export.ExportType;

import java.util.List;
import java.util.Map;

/**
 * Body of POST /api/v1/exports. Dates are in the interface timezone, as in the search endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
public class ExportRequest {

    private ExportType type;

    /** yyyy-MM-dd HH:mm:ss, inclusive. */
    private String startFrom;

    /** yyyy-MM-dd HH:mm:ss, inclusive. */
    private String startTo;

    /** Filter name (e.g. transactionId, state, payerId) to accepted values. Empty lists are ignored. */
    private Map<String, List<String>> filters;
}
