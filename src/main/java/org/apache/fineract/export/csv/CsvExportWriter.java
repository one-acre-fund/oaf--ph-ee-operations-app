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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Writes rows as RFC 4180 CSV (UTF-8, CRLF, quoted when needed). The header is written on creation.
 */
public class CsvExportWriter<T> implements Closeable {

    private static final char BOM = '﻿';

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
        List<Object> values = new ArrayList<>(columns.size());
        for (ExportColumn<T> column : columns) {
            values.add(toCell(column.getValue().apply(row)));
        }
        csvWriter.write(values);
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
        return value;
    }
}
