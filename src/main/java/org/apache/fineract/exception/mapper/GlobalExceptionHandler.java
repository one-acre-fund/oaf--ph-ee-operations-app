package org.apache.fineract.exception.mapper;


import org.apache.fineract.data.ApiGlobalErrorResponse;
import org.apache.fineract.exception.NoAuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiGlobalErrorResponse> handleAuthenticationException(AuthenticationException exception) {
        String defaultUserMessage = exception.getMessage() != null ? exception.getMessage() : "Authentication failed";
        log.warn("Exception: {}, Message: {}", exception.getClass().getName(), defaultUserMessage);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiGlobalErrorResponse.unauthorized(defaultUserMessage));
    }

    @ExceptionHandler(NoAuthorizationException.class)
    public ResponseEntity<ApiGlobalErrorResponse> handleNoAuthorizationException(NoAuthorizationException exception) {
        String defaultUserMessage = exception.getMessage() != null ? exception.getMessage() : "Authorization failed";
        log.warn("Exception: {}, Message: {}", exception.getClass().getName(), defaultUserMessage);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiGlobalErrorResponse.forbidden(defaultUserMessage));
    }
}
