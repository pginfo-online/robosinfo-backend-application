package com.ecommerce.marketplace.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateOperationException extends MarketplaceException {

    public DuplicateOperationException(String message) {
        super(message, "DUPLICATE_OPERATION", HttpStatus.CONFLICT);
    }
}
