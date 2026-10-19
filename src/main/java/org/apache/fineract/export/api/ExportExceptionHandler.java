package org.apache.fineract.export.api;

import org.apache.fineract.export.ExportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
