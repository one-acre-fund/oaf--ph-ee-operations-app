package org.apache.fineract.export.csv;

import org.apache.fineract.operations.Transfer;
import org.apache.fineract.operations.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExportWriterTest {

    private static final List<ExportColumn<Transfer>> COLUMNS = Arrays.asList(
            ExportColumn.of("TRANSACTIONID", Transfer::getTransactionId),
            ExportColumn.of("STATUS", Transfer::getStatus),
            ExportColumn.of("AMOUNT", Transfer::getAmount),
            ExportColumn.of("ERRORINFORMATION", Transfer::getErrorInformation),
            ExportColumn.of("STARTEDAT", Transfer::getStartedAt),
            ExportColumn.of("COMPLETEDAT", Transfer::getCompletedAt));

    private final ExportDateFormatter dates = new ExportDateFormatter("UTC");

    @Test
    @DisplayName("Writes the header, escapes values per RFC 4180 and leaves missing values empty")
    void writesEscapedRows() throws IOException {
        Transfer completed = new Transfer();
        completed.setTransactionId("tx-1");
        completed.setStatus(TransferStatus.COMPLETED);
        completed.setAmount(new BigDecimal("1E+3"));
        completed.setStartedAt(new Date(0));
        completed.setCompletedAt(new Date(1000));
        Transfer failed = new Transfer();
        failed.setTransactionId("tx-2");
        failed.setErrorInformation("{\"msg\": \"timeout, retry\"}");

        String csv = write(Arrays.asList(completed, failed), false);

        assertEquals("TRANSACTIONID,STATUS,AMOUNT,ERRORINFORMATION,STARTEDAT,COMPLETEDAT\r\n"
                + "tx-1,COMPLETED,1000,,1970-01-01T00:00:00Z,1970-01-01T00:00:01Z\r\n"
                + "tx-2,,,\"{\"\"msg\"\": \"\"timeout, retry\"\"}\",,\r\n", csv);
    }

    @Test
    @DisplayName("Starts with a UTF-8 BOM when asked")
    void writesBom() throws IOException {
        String csv = write(Collections.emptyList(), true);

        assertTrue(csv.startsWith("\uFEFFTRANSACTIONID,"), csv);
    }

    private String write(List<Transfer> rows, boolean bom) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CsvExportWriter<Transfer> writer = new CsvExportWriter<>(out, COLUMNS, dates, bom)) {
            writer.writeAll(rows);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
