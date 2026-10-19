package org.apache.fineract.export.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ExportErrorResponse {

    private final String error;
    private final String message;
    private final int status;
    private final List<String> details;

    public ExportErrorResponse(String error, String message, int status, List<String> details) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.details = details;
    }
}
