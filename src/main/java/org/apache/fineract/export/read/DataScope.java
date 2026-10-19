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

    private static final List<String> WILDCARD = Collections.singletonList("*");

    /**
     * Converts an assignment list as stored on the user. Only exactly ["*"] means no restriction, as in the
     * search endpoints; a list such as ["KES", "*"] is a literal restriction.
     */
    public static List<String> restriction(List<String> assigned) {
        if (assigned == null) {
            return Collections.emptyList();
        }
        return WILDCARD.equals(assigned) ? null : assigned;
    }
}
