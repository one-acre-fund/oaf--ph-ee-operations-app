package org.apache.fineract.retention;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariablePartitionsTest {

    private static final List<String> V56 = Arrays.asList("p2026_09", "p2026_10", "p2026_11", "p2026_12",
            "p2027_01", "p2027_02", "p2027_03", "pmax");

    @Test
    @DisplayName("Names and bounds match the V56 script")
    void namesAndBounds() {
        assertEquals("p2025_11", VariablePartitions.name(YearMonth.of(2025, 11)));
        assertEquals(Optional.of(YearMonth.of(2027, 3)), VariablePartitions.month("p2027_03"));
        assertEquals(Optional.empty(), VariablePartitions.month("pmax"));
        assertEquals(1764547200000L, VariablePartitions.upperBoundMillis(YearMonth.of(2025, 11)));
        assertEquals(1806537600000L, VariablePartitions.upperBoundMillis(YearMonth.of(2027, 3)));
    }

    @Test
    @DisplayName("Adds only the months missing after the newest partition, up to the months ahead")
    void toAdd() {
        assertEquals(Collections.emptyList(), VariablePartitions.toAdd(V56, YearMonth.of(2026, 12), 3));
        assertEquals(Arrays.asList(YearMonth.of(2027, 4), YearMonth.of(2027, 5)),
                VariablePartitions.toAdd(V56, YearMonth.of(2027, 2), 3));
        // a table with only pmax gets the current month and the months ahead
        assertEquals(Arrays.asList(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1)),
                VariablePartitions.toAdd(Collections.singletonList("pmax"), YearMonth.of(2026, 11), 2));
    }

    @Test
    @DisplayName("Drops the months entirely older than the retention window, oldest first, never pmax")
    void toDrop() {
        // on 2027-11-02 with 12 months kept: 2026-11 .. 2027-10 stay, 2026-10 and older go
        assertEquals(Arrays.asList("p2026_09", "p2026_10"), VariablePartitions.toDrop(V56, YearMonth.of(2027, 11), 12));
        assertEquals(Collections.emptyList(), VariablePartitions.toDrop(V56, YearMonth.of(2027, 9), 12));
        assertEquals(Arrays.asList("p2026_09", "p2026_10", "p2026_11", "p2026_12"),
                VariablePartitions.toDrop(V56, YearMonth.of(2027, 7), 6));
    }

    @Test
    void checkedAcceptsOnlyPartitionNames() {
        assertEquals("p2026_11", VariablePartitions.checked("p2026_11"));
        assertEquals("pmax", VariablePartitions.checked("pmax"));
        assertThrows(IllegalArgumentException.class, () -> VariablePartitions.checked("p2026_11 , pmax"));
        assertThrows(IllegalArgumentException.class, () -> VariablePartitions.checked("variables"));
    }
}
