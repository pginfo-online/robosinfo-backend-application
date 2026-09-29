package com.ecommerce.marketplace.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all business/domain exceptions in the marketplace.
 * Maps to RFC 7807 Problem Details in the global error handler.
 */
public abstract class MarketplaceException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected MarketplaceException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected MarketplaceException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
