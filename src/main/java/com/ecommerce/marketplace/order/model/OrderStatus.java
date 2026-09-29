package com.ecommerce.marketplace.order.model;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case CREATED -> next == PAYMENT_PENDING || next == PAID || next == CANCELLED;
            case PAYMENT_PENDING -> next == PAID || next == CANCELLED;
            case PAID -> next == PROCESSING || next == CANCELLED || next == REFUNDED;
            case PROCESSING -> next == SHIPPED || next == CANCELLED;
            case SHIPPED -> next == DELIVERED;
            case DELIVERED -> next == REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };
    }
}
