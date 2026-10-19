package org.apache.fineract.export.read;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * The requester's assignments. Each list is either null (no restriction) or the allowed values;
 * an empty list allows nothing.
 */
@Getter
@Builder
public class DataScope {

    public static final DataScope UNRESTRICTED = DataScope.builder().build();

    private final List<String> payeePartyIds;
    private final List<String> currencies;
    private final List<String> payeePartyIdTypes;

    /**
     * Converts an assignment list as stored on the user: ["*"] means no restriction.
     */
    public static List<String> restriction(List<String> assigned) {
        if (assigned == null) {
            return Collections.emptyList();
        }
        return assigned.contains("*") ? null : assigned;
    }
}
