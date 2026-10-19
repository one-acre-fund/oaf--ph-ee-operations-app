package org.apache.fineract.export.csv;

import lombok.Getter;

import java.util.function.Function;

@Getter
public final class ExportColumn<T> {

    private final String header;
    private final Function<T, Object> value;

    private ExportColumn(String header, Function<T, Object> value) {
        this.header = header;
        this.value = value;
    }

    public static <T> ExportColumn<T> of(String header, Function<T, Object> value) {
        return new ExportColumn<>(header, value);
    }
}
