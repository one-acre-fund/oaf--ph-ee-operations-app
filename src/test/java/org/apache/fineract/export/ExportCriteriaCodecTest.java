package org.apache.fineract.export;

import org.apache.fineract.export.read.DataScope;
import org.apache.fineract.export.read.ExportCriteria;
import org.apache.fineract.operations.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportCriteriaCodecTest {

    private final ExportCriteriaCodec codec = new ExportCriteriaCodec();

    @Test
    @DisplayName("Criteria and scope survive a round trip through the job's JSON columns")
    void roundTrip() {
        ExportCriteria criteria = ExportCriteria.builder()
                .startFrom(new Date(1000)).startTo(new Date(2000)).direction(Sort.Direction.ASC)
                .filters(Collections.singletonMap(Filter.STATE, Arrays.asList("FAILED", "COMPLETED")))
                .build();
        DataScope scope = DataScope.builder().currencies(Collections.singletonList("KES")).build();

        ExportCriteria read = codec.fromJson(codec.filtersToJson(criteria), codec.scopeToJson(scope));

        assertEquals(new Date(1000), read.getStartFrom());
        assertEquals(new Date(2000), read.getStartTo());
        assertEquals(Sort.Direction.ASC, read.getDirection());
        assertEquals(Arrays.asList("FAILED", "COMPLETED"), read.getFilters().get(Filter.STATE));
        assertEquals(Collections.singletonList("KES"), read.getScope().getCurrencies());
        assertNull(read.getScope().getPayeePartyIds());
    }

    @Test
    @DisplayName("Missing values default to no dates, newest first, no filters, unrestricted")
    void defaults() {
        ExportCriteria read = codec.fromJson("{}", "{}");

        assertNull(read.getStartFrom());
        assertNull(read.getStartTo());
        assertEquals(Sort.Direction.DESC, read.getDirection());
        assertTrue(read.getFilters().isEmpty());
        assertNull(read.getScope().getCurrencies());
    }

    @Test
    @DisplayName("Unreadable stored JSON is an error, not an unrestricted export")
    void unreadableJson() {
        assertThrows(UncheckedIOException.class, () -> codec.fromJson("not json", "{}"));
    }
}
