package com.ecommerce.marketplace.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends MarketplaceException {

    public BusinessRuleException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.CONFLICT);
    }

    public BusinessRuleException(String message) {
        super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.CONFLICT);
    }
}
