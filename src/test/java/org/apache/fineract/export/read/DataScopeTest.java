package org.apache.fineract.export.read;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataScopeTest {

    @Test
    @DisplayName("Only exactly [\"*\"] means no restriction, as in the search endpoints")
    void singletonWildcardIsUnrestricted() {
        assertNull(DataScope.restriction(Collections.singletonList("*")));
    }

    @Test
    @DisplayName("A list that mixes values and * stays a literal restriction")
    void mixedListIsRestricted() {
        assertEquals(Arrays.asList("KES", "*"), DataScope.restriction(Arrays.asList("KES", "*")));
    }

    @Test
    @DisplayName("Missing or empty assignments allow nothing")
    void missingOrEmptyAllowsNothing() {
        assertEquals(Collections.emptyList(), DataScope.restriction(null));
        assertEquals(Collections.emptyList(), DataScope.restriction(Collections.emptyList()));
    }
}
