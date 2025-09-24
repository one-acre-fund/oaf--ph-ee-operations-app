package org.apache.fineract.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

@Component
public class DateUtil {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${interface.timezone}")
    public String interfaceTimezone;

    public String getUTCFormat(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        ZoneId interfaceZone = ZoneId.of(interfaceTimezone);
        ZonedDateTime interfaceDateTime = ZonedDateTime.of(localDateTime, interfaceZone);
        ZonedDateTime gmtDateTime = interfaceDateTime.withZoneSameInstant(ZoneId.of("GMT"));
        DateTimeFormatter gmtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String gmtDateTimeString = gmtDateTime.format(gmtFormatter);
        return gmtDateTimeString;
    }

    public LocalDateTime getLocalDateTimeOfTenant(HttpServletRequest request) {
        ZoneId zone = null;
        if (request != null) {
            TimeZone timeZone = RequestContextUtils.getTimeZone(request);
            if (timeZone != null)
                zone = timeZone.toZoneId();
        }
        return LocalDateTime.now(zone != null ? zone : ZoneId.systemDefault());
    }
    public static LocalDateTime parseDateTime(String dateTimeStr, DateTimeFormatter formatter) {
        try {
            // Attempt to parse as LocalDateTime
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            // If parsing as LocalDateTime fails, try parsing as LocalDate and convert to LocalDateTime
            LocalDate date = LocalDate.parse(dateTimeStr, formatter);
            return date.atStartOfDay();
        }
    }
}
