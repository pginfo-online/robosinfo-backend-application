package com.ecommerce.marketplace.order.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    @DisplayName("Should allow valid order status transitions")
    void testValidTransitions() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.PAYMENT_PENDING));
        assertTrue(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Should reject invalid order status transitions")
    void testInvalidTransitions() {
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED));
    }
}
