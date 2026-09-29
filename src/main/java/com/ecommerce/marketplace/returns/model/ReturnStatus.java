package com.ecommerce.marketplace.returns.model;

public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    PICKUP_SCHEDULED,
    PICKED_UP,
    IN_TRANSIT,
    RECEIVED_AT_WAREHOUSE,
    QC_PASSED,
    QC_FAILED,
    REFUNDED,
    REPLACED,
    CANCELLED
}
