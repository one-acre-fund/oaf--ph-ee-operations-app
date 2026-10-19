package org.apache.fineract.export;

import org.apache.fineract.export.api.ExportRequest;
import org.apache.fineract.export.read.ExportCriteria;
import org.apache.fineract.operations.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFilterValidatorTest {

    private final ExportFilterValidator validator = new ExportFilterValidator("Africa/Nairobi");

    @Test
    @DisplayName("Converts dates from the interface timezone and maps filter names case-insensitively")
    void validRequest() {
        ExportCriteria criteria = validator.validate(request(ExportType.TRANSFERS, "2026-10-15 00:00:00",
                "2026-10-15 23:59:59", filters("transactionId", Arrays.asList("t1", "t2"), "STATE",
                        Collections.singletonList("completed"))));

        assertEquals(Instant.parse("2026-10-14T21:00:00Z"), criteria.getStartFrom().toInstant());
        assertEquals(Instant.parse("2026-10-15T20:59:59Z"), criteria.getStartTo().toInstant());
        assertEquals(Arrays.asList("t1", "t2"), criteria.getFilters().get(Filter.TRANSACTIONID));
        assertEquals(Collections.singletonList("completed"), criteria.getFilters().get(Filter.STATE));
    }

    @Test
    @DisplayName("No dates and no filters means everything; empty filter lists are ignored")
    void emptyRequest() {
        ExportCriteria criteria = validator.validate(request(ExportType.TRANSACTION_REQUESTS, null, "",
                filters("externalId", Collections.emptyList(), "payerId", null)));

        assertNull(criteria.getStartFrom());
        assertNull(criteria.getStartTo());
        assertTrue(criteria.getFilters().isEmpty());
    }

    @Test
    @DisplayName("Reports every problem at once with 400, dropping nothing silently")
    void reportsAllErrors() {
        Map<String, List<String>> filters = filters("bogus", Collections.singletonList("x"),
                "errorDescription", Collections.singletonList("x"));
        filters.put("state", Collections.singletonList("DONE"));
        filters.put("workflowInstanceKey", Arrays.asList("12", "abc"));
        filters.put("payerId", Arrays.asList("", null));

        ExportRequest request = request(ExportType.TRANSFERS, "15/10/2026", "2026-10-15 23:59:59", filters);

        ExportException e = assertThrows(ExportException.class, () -> validator.validate(request));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        List<String> details = e.getDetails();
        assertEquals(7, details.size(), details.toString());
        assertTrue(details.get(0).startsWith("startFrom: expected yyyy-MM-dd HH:mm:ss"));
        assertTrue(details.get(1).startsWith("filters.bogus: unknown filter"));
        assertEquals("filters.errorDescription: not supported for TRANSFERS", details.get(2));
        assertEquals("filters.state: unknown state 'DONE'", details.get(3));
        assertEquals("filters.workflowInstanceKey: not a number 'abc'", details.get(4));
        assertEquals("filters.payerId: empty value", details.get(5));
        assertEquals("filters.payerId: empty value", details.get(6));
    }

    @Test
    @DisplayName("Without a type, still reports filter problems that do not depend on it")
    void missingTypeStillChecksFilters() {
        Map<String, List<String>> filters = filters("bogus", Collections.singletonList("x"),
                "payerId", Collections.singletonList(""));
        filters.put("state", Collections.singletonList("anything"));
        ExportRequest request = request(null, null, null, filters);

        ExportException e = assertThrows(ExportException.class, () -> validator.validate(request));

        List<String> details = e.getDetails();
        assertEquals(3, details.size(), details.toString());
        assertTrue(details.get(0).startsWith("type: required"));
        assertTrue(details.contains("filters.payerId: empty value"), details.toString());
        assertTrue(details.stream().anyMatch(d -> d.startsWith("filters.bogus: unknown filter")), details.toString());
    }

    @Test
    @DisplayName("Rejects impossible dates instead of shifting them")
    void rejectsImpossibleDates() {
        ExportRequest request = request(ExportType.TRANSFERS, "2026-02-30 00:00:00", "2026-10-15 24:00:00", null);

        ExportException e = assertThrows(ExportException.class, () -> validator.validate(request));

        assertEquals(2, e.getDetails().size(), e.getDetails().toString());
        assertTrue(e.getDetails().get(0).startsWith("startFrom: expected yyyy-MM-dd HH:mm:ss"));
        assertTrue(e.getDetails().get(1).startsWith("startTo: expected yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    @DisplayName("Rejects a missing type, a reversed date range and a filter given twice")
    void rejectsTypeRangeAndDuplicates() {
        ExportRequest withoutType = request(null, null, null, null);
        ExportRequest reversedRange = request(ExportType.TRANSACTION_REQUESTS, "2026-10-16 00:00:00",
                "2026-10-15 00:00:00", null);
        ExportRequest stateTwice = request(ExportType.TRANSACTION_REQUESTS, null, null, filters("state",
                Collections.singletonList("failed"), "STATE", Collections.singletonList("accepted")));

        ExportException noType = assertThrows(ExportException.class, () -> validator.validate(withoutType));
        ExportException reversed = assertThrows(ExportException.class, () -> validator.validate(reversedRange));
        ExportException twice = assertThrows(ExportException.class, () -> validator.validate(stateTwice));

        assertEquals(1, noType.getDetails().size());
        assertTrue(noType.getDetails().get(0).startsWith("type: required"));
        assertEquals("startFrom: must not be after startTo", reversed.getDetails().get(0));
        assertEquals("filters.STATE: given more than once", twice.getDetails().get(0));
    }

    @Test
    @DisplayName("Accepts transaction-request states and non-numeric workflow keys for transaction requests")
    void transactionRequestValues() {
        ExportCriteria criteria = validator.validate(request(ExportType.TRANSACTION_REQUESTS, null, null,
                filters("state", Collections.singletonList("REJECTED"), "workflowInstanceKey",
                        Collections.singletonList("abc"))));

        assertEquals(2, criteria.getFilters().size());
    }

    static ExportRequest request(ExportType type, String from, String to, Map<String, List<String>> filters) {
        ExportRequest request = new ExportRequest();
        request.setType(type);
        request.setStartFrom(from);
        request.setStartTo(to);
        request.setFilters(filters);
        return request;
    }

    private static Map<String, List<String>> filters(String k1, List<String> v1, String k2, List<String> v2) {
        Map<String, List<String>> filters = new LinkedHashMap<>();
        filters.put(k1, v1);
        filters.put(k2, v2);
        return filters;
    }
}
