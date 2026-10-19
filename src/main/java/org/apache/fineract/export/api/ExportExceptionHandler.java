package org.apache.fineract.export.api;

import org.apache.fineract.export.ExportException;
import org.apache.fineract.export.ExportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;

@RestControllerAdvice(assignableTypes = ExportController.class)
public class ExportExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ExportErrorResponse> handle(ExportException exception) {
        logger.info("Export request refused: {} {}", exception.getStatus(), exception.getMessage());
        return ResponseEntity.status(exception.getStatus()).body(new ExportErrorResponse(
                exception.getStatus().getReasonPhrase(), exception.getMessage(), exception.getStatus().value(),
                exception.getDetails()));
    }

    /**
     * Malformed JSON or an unknown type fails while binding, before validation runs; answer in the same format.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExportErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        logger.info("Export request refused: unreadable body");
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ExportErrorResponse(status.getReasonPhrase(),
                "Request body is not valid JSON or has an unknown value; type must be one of "
                        + Arrays.toString(ExportType.values()), status.value(), null));
    }
}
