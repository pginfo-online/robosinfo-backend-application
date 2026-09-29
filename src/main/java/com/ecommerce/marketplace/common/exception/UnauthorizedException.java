package com.ecommerce.marketplace.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends MarketplaceException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String errorCode, String message) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
}
