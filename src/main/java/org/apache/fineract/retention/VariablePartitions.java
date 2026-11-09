package org.apache.fineract.retention;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Naming and arithmetic of the monthly partitions of the variables table (V56): pYYYY_MM holds the rows whose
 * TIMESTAMP (ms epoch, UTC) is before the first instant of the following month; pmax catches everything later.
 */
final class VariablePartitions {

    static final String PMAX = "pmax";
    private static final Pattern MONTH = Pattern.compile("p(\\d{4})_(\\d{2})");

    private VariablePartitions() {
    }

    /**
     * Returns the name unchanged if it is pmax or pYYYY_MM, and throws otherwise. Partition names cannot be bind
     * parameters, so every name that reaches SQL goes through here first.
     */
    static String checked(String partitionName) {
        if (PMAX.equals(partitionName) || MONTH.matcher(partitionName).matches()) {
            return partitionName;
        }
        throw new IllegalArgumentException("Not a variables partition name: " + partitionName);
    }

    static String name(YearMonth month) {
        return String.format("p%04d_%02d", month.getYear(), month.getMonthValue());
    }

    static Optional<YearMonth> month(String partitionName) {
        Matcher m = MONTH.matcher(partitionName);
        return m.matches() ? Optional.of(YearMonth.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))))
                : Optional.empty();
    }

    /** Exclusive upper bound of the month's partition: the first instant of the next month, in ms. */
    static long upperBoundMillis(YearMonth month) {
        return month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /** Months after the newest existing monthly partition, up to the current month plus {@code aheadMonths}. */
    static List<YearMonth> toAdd(List<String> existing, YearMonth current, int aheadMonths) {
        YearMonth newest = existing.stream().map(VariablePartitions::month)
                .filter(Optional::isPresent).map(Optional::get)
                .max(YearMonth::compareTo).orElse(current.minusMonths(1));
        List<YearMonth> missing = new ArrayList<>();
        for (YearMonth m = newest.plusMonths(1); !m.isAfter(current.plusMonths(aheadMonths)); m = m.plusMonths(1)) {
            missing.add(m);
        }
        return missing;
    }

    /** Monthly partitions entirely older than the retention window, oldest first. */
    static List<String> toDrop(List<String> existing, YearMonth current, int retentionMonths) {
        YearMonth newestToDrop = current.minusMonths(retentionMonths + 1L);
        List<String> old = new ArrayList<>();
        for (String name : existing) {
            month(name).filter(m -> !m.isAfter(newestToDrop)).ifPresent(m -> old.add(name));
        }
        old.sort(String::compareTo);
        return old;
    }
}
