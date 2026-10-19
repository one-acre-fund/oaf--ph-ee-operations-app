package org.apache.fineract.export.csv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Formats export dates as ISO-8601 with the offset of the interface timezone, e.g. 2026-10-15T14:30:00+05:30.
 */
@Component
public class ExportDateFormatter {

    private final DateTimeFormatter formatter;

    @Autowired
    public ExportDateFormatter(@Value("${interface.timezone}") String timezone) {
        this(ZoneId.of(timezone));
    }

    public ExportDateFormatter(ZoneId zone) {
        this.formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zone);
    }

    /**
     * @return the formatted date, or null (an empty CSV cell) when the date is missing
     */
    public String format(Date date) {
        return date == null ? null : formatter.format(Instant.ofEpochMilli(date.getTime()));
    }
}
