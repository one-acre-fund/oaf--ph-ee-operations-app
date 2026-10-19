package org.apache.fineract.export.csv;

import org.apache.fineract.operations.TransactionRequest;
import org.apache.fineract.operations.Transfer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportColumnsTest {

    @Test
    @DisplayName("Transfer columns match the legacy export header (entity fields in upper case)")
    void transferHeadersMatchLegacy() {
        assertEquals(legacyHeaders(Transfer.class), headers(ExportColumns.TRANSFERS));
    }

    @Test
    @DisplayName("Transaction request columns match the legacy export header, without the variables list")
    void transactionRequestHeadersMatchLegacy() {
        List<String> legacy = legacyHeaders(TransactionRequest.class);
        legacy.remove("VARIABLES");
        assertEquals(legacy, headers(ExportColumns.TRANSACTION_REQUESTS));
    }

    /** What CsvWriter.generateNameMapping produced for the old exports. */
    private static List<String> legacyHeaders(Class<?> entity) {
        return Arrays.stream(entity.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic() && !f.getName().startsWith("_"))
                .map(Field::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    private static <T> List<String> headers(List<ExportColumn<T>> columns) {
        return columns.stream().map(ExportColumn::getHeader).collect(Collectors.toList());
    }
}
