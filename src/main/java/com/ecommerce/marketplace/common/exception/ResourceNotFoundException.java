package com.ecommerce.marketplace.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends MarketplaceException {

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(
            resourceType + " not found: " + identifier,
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String message) {
        super(
            message,
            "RESOURCE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }
}
