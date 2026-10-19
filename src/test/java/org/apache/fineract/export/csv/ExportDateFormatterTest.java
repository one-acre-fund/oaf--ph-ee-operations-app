package org.apache.fineract.export.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExportDateFormatterTest {

    private static final Date NOON_UTC = new Date(1760529600000L); // 2025-10-15T12:00:00Z

    @Test
    @DisplayName("Formats as ISO-8601 with the interface timezone offset")
    void formatsWithOffset() {
        assertEquals("2025-10-15T17:30:00+05:30", new ExportDateFormatter("Asia/Kolkata").format(NOON_UTC));
        assertEquals("2025-10-15T15:00:00+03:00", new ExportDateFormatter(ZoneId.of("Africa/Nairobi")).format(NOON_UTC));
        assertEquals("2025-10-15T12:00:00Z", new ExportDateFormatter("UTC").format(NOON_UTC));
    }

    @Test
    @DisplayName("A missing date becomes an empty cell")
    void nullIsNull() {
        assertNull(new ExportDateFormatter("UTC").format(null));
    }
}
