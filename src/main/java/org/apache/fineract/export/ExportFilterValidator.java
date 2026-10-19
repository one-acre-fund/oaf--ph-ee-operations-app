package org.apache.fineract.export;

import org.apache.fineract.export.api.ExportRequest;
import org.apache.fineract.export.read.ExportCriteria;
import org.apache.fineract.operations.Filter;
import org.apache.fineract.operations.TransactionRequestState;
import org.apache.fineract.operations.TransferStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a request into validated criteria. Every problem is reported at once (400) and nothing is silently dropped,
 * except filters with an empty list, which mean "not filtered" as in the legacy exports.
 */
@Component
public class ExportFilterValidator {

    private static final String FILTERS = "filters.";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    private static final Set<Filter> TRANSFER_FILTERS = EnumSet.of(Filter.TRANSACTIONID, Filter.PAYERID,
            Filter.PAYEEID, Filter.WORKFLOWINSTANCEKEY, Filter.STATE, Filter.CLIENTCORRELATIONID, Filter.PAYERDFSPID);

    private final ZoneId interfaceZone;

    public ExportFilterValidator(@Value("${interface.timezone}") String interfaceTimezone) {
        this.interfaceZone = ZoneId.of(interfaceTimezone);
    }

    public ExportCriteria validate(ExportRequest request) {
        List<String> errors = new ArrayList<>();
        ExportType type = request.getType();
        if (type == null) {
            errors.add("type: required, one of " + Arrays.toString(ExportType.values()));
        }
        Date startFrom = parseDate("startFrom", request.getStartFrom(), errors);
        Date startTo = parseDate("startTo", request.getStartTo(), errors);
        if (startFrom != null && startTo != null && startFrom.after(startTo)) {
            errors.add("startFrom: must not be after startTo");
        }
        Map<Filter, List<String>> filters = type == null ? new EnumMap<>(Filter.class)
                : parseFilters(type, request.getFilters(), errors);

        if (!errors.isEmpty()) {
            throw new ExportException(HttpStatus.BAD_REQUEST, "Invalid export request", errors);
        }
        return ExportCriteria.builder().startFrom(startFrom).startTo(startTo).filters(filters).build();
    }

    private Date parseDate(String field, String value, List<String> errors) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Date.from(LocalDateTime.parse(value, DATE_FORMAT).atZone(interfaceZone).toInstant());
        } catch (DateTimeParseException e) {
            errors.add(field + ": expected yyyy-MM-dd HH:mm:ss, got '" + value + "'");
            return null;
        }
    }

    private Map<Filter, List<String>> parseFilters(ExportType type, Map<String, List<String>> requested,
                                                   List<String> errors) {
        Map<Filter, List<String>> filters = new EnumMap<>(Filter.class);
        if (requested == null) {
            return filters;
        }
        for (Map.Entry<String, List<String>> entry : requested.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            Filter filter = parseFilterName(name);
            if (filter == null) {
                errors.add(FILTERS + name + ": unknown filter, one of " + Arrays.toString(Filter.values()));
            } else if (type == ExportType.TRANSFERS && !TRANSFER_FILTERS.contains(filter)) {
                errors.add(FILTERS + name + ": not supported for " + type);
            } else if (filters.containsKey(filter)) {
                errors.add(FILTERS + name + ": given more than once");
            } else if (values != null && !values.isEmpty()) {
                validateValues(type, filter, name, values, errors);
                filters.put(filter, values);
            }
        }
        return filters;
    }

    private static Filter parseFilterName(String name) {
        try {
            return Filter.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void validateValues(ExportType type, Filter filter, String name, List<String> values,
                                       List<String> errors) {
        for (String value : values) {
            if (value == null || value.isEmpty()) {
                errors.add(FILTERS + name + ": empty value");
            } else if (filter == Filter.STATE && !isState(type, value)) {
                errors.add(FILTERS + name + ": unknown state '" + value + "'");
            } else if (filter == Filter.WORKFLOWINSTANCEKEY && type == ExportType.TRANSFERS && !isNumber(value)) {
                errors.add(FILTERS + name + ": not a number '" + value + "'");
            }
        }
    }

    private static boolean isState(ExportType type, String value) {
        Class<? extends Enum<?>> states = type == ExportType.TRANSFERS ? TransferStatus.class
                : TransactionRequestState.class;
        return Arrays.stream(states.getEnumConstants()).anyMatch(s -> s.name().equalsIgnoreCase(value));
    }

    private static boolean isNumber(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
