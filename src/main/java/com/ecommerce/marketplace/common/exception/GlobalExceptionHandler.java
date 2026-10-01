package com.ecommerce.marketplace.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler — converts all exceptions to RFC 7807 Problem Details.
 *
 * IMPORTANT: Spring Security's ExceptionTranslationFilter only handles AccessDeniedException
 * that propagate through the filter chain. When @PreAuthorize throws AuthorizationDeniedException
 * (a subclass of AccessDeniedException) inside a controller method, it bypasses the filter chain
 * and lands here. We must return 403 — NOT 500 — for both.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MarketplaceException.class)
    public ProblemDetail handleMarketplaceException(MarketplaceException ex) {
        log.warn("Business error: {} - {}", ex.getErrorCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            ex.getHttpStatus(), ex.getMessage()
        );
        problem.setType(URI.create("https://api.marketplace.com/errors/" + ex.getErrorCode().toLowerCase().replace("_", "-")));
        problem.setTitle(ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", ex.getErrorCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed"
        );
        problem.setType(URI.create("https://api.marketplace.com/errors/validation-failed"));
        problem.setTitle("VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    /**
     * Handles both legacy AccessDeniedException and Spring Security 6's
     * AuthorizationDeniedException (thrown by @PreAuthorize when the user lacks the
     * required role). Returns 403 — NOT 500.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ProblemDetail handleAccessDeniedException(Exception ex) {
        // Log at DEBUG only — this is expected for unauthenticated/unauthorized users
        log.debug("Access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN, "You do not have permission to perform this action"
        );
        problem.setType(URI.create("https://api.marketplace.com/errors/access-denied"));
        problem.setTitle("ACCESS_DENIED");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"
        );
        problem.setType(URI.create("https://api.marketplace.com/errors/internal-server-error"));
        problem.setTitle("INTERNAL_SERVER_ERROR");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
