package com.ecommerce.marketplace.inventory.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTest {

    @Test
    @DisplayName("Should correctly calculate available quantity")
    void testAvailableQtyCalculation() {
        Inventory inv = Inventory.builder()
            .variantId(UUID.randomUUID())
            .sellerId(UUID.randomUUID())
            .warehouseId(UUID.randomUUID())
            .physicalQty(100)
            .reservedQty(20)
            .damagedQty(5)
            .build();

        assertEquals(75, inv.getAvailableQty());
    }

    @Test
    @DisplayName("Available quantity should be 0 when reserved equals physical")
    void testZeroAvailableQty() {
        Inventory inv = Inventory.builder()
            .variantId(UUID.randomUUID())
            .sellerId(UUID.randomUUID())
            .warehouseId(UUID.randomUUID())
            .physicalQty(50)
            .reservedQty(50)
            .damagedQty(0)
            .build();

        assertEquals(0, inv.getAvailableQty());
    }
}
