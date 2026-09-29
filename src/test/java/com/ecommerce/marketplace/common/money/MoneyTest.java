package com.ecommerce.marketplace.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("Should create Money with paisa amount and convert to Rupees")
    void testCreateMoney() {
        Money m = Money.ofPaisa(1000L);
        assertEquals(1000L, m.paisa());
        assertEquals(new BigDecimal("10.00"), m.toRupees());
        assertEquals("₹10.00", m.toString());
    }

    @Test
    @DisplayName("Should correctly add and subtract Money")
    void testAddAndSubtract() {
        Money m1 = Money.ofPaisa(5000L);
        Money m2 = Money.ofPaisa(2500L);

        Money sum = m1.add(m2);
        assertEquals(7500L, sum.paisa());

        Money diff = m1.subtract(m2);
        assertEquals(2500L, diff.paisa());
    }

    @Test
    @DisplayName("Should correctly calculate percentage and multiplication")
    void testPercentageAndMultiply() {
        Money m = Money.ofPaisa(10000L); // Rs. 100
        Money tenPercent = m.percentage(new BigDecimal("10"));
        assertEquals(1000L, tenPercent.paisa()); // Rs. 10

        Money doubled = m.multiply(2);
        assertEquals(20000L, doubled.paisa()); // Rs. 200
    }

    @Test
    @DisplayName("Should correctly evaluate relational operators")
    void testComparisons() {
        Money m1 = Money.ofPaisa(5000L);
        Money m2 = Money.ofPaisa(2500L);

        assertTrue(m1.isGreaterThan(m2));
        assertFalse(m2.isGreaterThan(m1));
        assertTrue(m2.isLessThanOrEqual(m1));
        assertTrue(m1.isPositive());
        assertFalse(m1.isZero());
        assertTrue(Money.ZERO.isZero());
    }
}
