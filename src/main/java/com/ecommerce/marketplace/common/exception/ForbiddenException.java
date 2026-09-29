package com.ecommerce.marketplace.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends MarketplaceException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }
}
