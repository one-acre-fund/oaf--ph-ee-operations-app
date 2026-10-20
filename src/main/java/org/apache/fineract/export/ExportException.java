package org.apache.fineract.export;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

@Getter
public class ExportException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> details;

    public ExportException(HttpStatus status, String message) {
        this(status, message, Collections.emptyList());
    }

    public ExportException(HttpStatus status, String message, List<String> details) {
        super(message);
        this.status = status;
        this.details = details;
    }

    public static ExportException notFound(String jobId) {
        return new ExportException(HttpStatus.NOT_FOUND, "Export " + jobId + " not found");
    }
}
