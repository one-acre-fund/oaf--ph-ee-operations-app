package org.apache.fineract.export.csv;

import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Writes rows as RFC 4180 CSV (UTF-8, CRLF, quoted when needed). The header is written on creation.
 * Text that a spreadsheet would treat as a formula is prefixed with an apostrophe.
 */
public class CsvExportWriter<T> implements Closeable {

    private static final char BOM = '\uFEFF';
    private static final String FORMULA_START = "=+-@\t\r";
    private static final Pattern PLAIN_NUMBER = Pattern.compile("[+-]?\\d+(\\.\\d+)?");

    private final ICsvListWriter csvWriter;
    private final List<ExportColumn<T>> columns;
    private final ExportDateFormatter dateFormatter;

    /**
     * @param withBom write a UTF-8 byte order mark first, so Excel detects the encoding
     */
    public CsvExportWriter(OutputStream out, List<ExportColumn<T>> columns, ExportDateFormatter dateFormatter,
                           boolean withBom) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        if (withBom) {
            writer.write(BOM);
        }
        this.csvWriter = new CsvListWriter(writer, CsvPreference.STANDARD_PREFERENCE);
        this.columns = columns;
        this.dateFormatter = dateFormatter;
        csvWriter.writeHeader(columns.stream().map(ExportColumn::getHeader).toArray(String[]::new));
    }

    public void write(T row) throws IOException {
        csvWriter.write(columns.stream()
                .map(column -> toCell(column.getValue().apply(row)))
                .collect(Collectors.toList()));
    }

    public void writeAll(List<T> rows) throws IOException {
        for (T row : rows) {
            write(row);
        }
    }

    @Override
    public void close() throws IOException {
        csvWriter.close();
    }

    private Object toCell(Object value) {
        if (value instanceof Date) {
            return dateFormatter.format((Date) value);
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof String) {
            return neutraliseFormula((String) value);
        }
        return value;
    }

    /**
     * Prefixes text that a spreadsheet would run as a formula (starting with =, +, -, @, tab or carriage return)
     * with an apostrophe, so it is shown as text. Plain numbers such as +254712345678 or -12.5 are left as they are.
     */
    static String neutraliseFormula(String value) {
        if (value.isEmpty() || FORMULA_START.indexOf(value.charAt(0)) < 0 || PLAIN_NUMBER.matcher(value).matches()) {
            return value;
        }
        return "'" + value;
    }
}
