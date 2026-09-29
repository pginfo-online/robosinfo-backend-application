package com.ecommerce.marketplace.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object for monetary amounts.
 * All money is stored internally as paisa (1 INR = 100 paisa) to avoid floating-point errors.
 * NEVER use double/float for money calculations.
 */
public record Money(long paisa) {

    public static final Money ZERO = new Money(0);

    public static Money ofPaisa(long paisa) {
        return new Money(paisa);
    }

    public static Money ofRupees(BigDecimal rupees) {
        return new Money(
            rupees.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        );
    }

    public static Money ofRupees(String rupees) {
        return ofRupees(new BigDecimal(rupees));
    }

    public BigDecimal toRupees() {
        return BigDecimal.valueOf(paisa)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public Money add(Money other) {
        return new Money(this.paisa + other.paisa);
    }

    public Money subtract(Money other) {
        return new Money(this.paisa - other.paisa);
    }

    public Money multiply(int quantity) {
        return new Money(this.paisa * quantity);
    }

    /**
     * Calculate percentage. E.g., percentage(10) returns 10% of this amount.
     * Rounds to nearest paisa.
     */
    public Money percentage(BigDecimal percent) {
        long result = BigDecimal.valueOf(this.paisa)
            .multiply(percent)
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            .longValueExact();
        return new Money(result);
    }

    public boolean isPositive() {
        return paisa > 0;
    }

    public boolean isZero() {
        return paisa == 0;
    }

    public boolean isGreaterThan(Money other) {
        return this.paisa > other.paisa;
    }

    public boolean isLessThanOrEqual(Money other) {
        return this.paisa <= other.paisa;
    }

    @Override
    public String toString() {
        return "₹" + toRupees().toPlainString();
    }
}
