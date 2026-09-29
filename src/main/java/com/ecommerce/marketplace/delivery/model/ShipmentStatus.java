package com.ecommerce.marketplace.delivery.model;

public enum ShipmentStatus {
    CREATED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    CANCELLED,
    RETURNED;

    public boolean canTransitionTo(ShipmentStatus next) {
        return switch (this) {
            case CREATED -> next == ASSIGNED || next == CANCELLED;
            case ASSIGNED -> next == PICKED_UP || next == CANCELLED;
            case PICKED_UP -> next == IN_TRANSIT || next == OUT_FOR_DELIVERY;
            case IN_TRANSIT -> next == OUT_FOR_DELIVERY || next == FAILED;
            case OUT_FOR_DELIVERY -> next == DELIVERED || next == FAILED;
            case FAILED -> next == OUT_FOR_DELIVERY || next == RETURNED || next == CANCELLED;
            case DELIVERED, CANCELLED, RETURNED -> false;
        };
    }
}
