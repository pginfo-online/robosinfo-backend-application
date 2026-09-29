package com.ecommerce.marketplace.promotion.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.promotion.dto.CouponValidationResponse;
import com.ecommerce.marketplace.promotion.model.Coupon;
import com.ecommerce.marketplace.promotion.model.DiscountType;
import com.ecommerce.marketplace.promotion.repository.CouponRepository;
import com.ecommerce.marketplace.promotion.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponUsageRepository couponUsageRepository;

    @InjectMocks
    private PromotionService promotionService;

    private UUID customerId;
    private Coupon percentCoupon;
    private Coupon flatCoupon;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        // 20% off with max cap 500 Rs (50000 paisa)
        percentCoupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("SAVE20")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(20L)
                .minOrderValuePaisa(100000L) // Min Rs. 1,000
                .maxDiscountCapPaisa(50000L)  // Max cap Rs. 500
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .isActive(true)
                .timesUsed(10)
                .totalUsageLimit(1000)
                .usageLimitPerUser(2)
                .build();

        // Flat Rs. 300 (30000 paisa) off
        flatCoupon = Coupon.builder()
                .id(UUID.randomUUID())
                .code("FLAT300")
                .discountType(DiscountType.FLAT_AMOUNT)
                .discountValue(30000L)
                .minOrderValuePaisa(100000L)
                .startDate(Instant.now().minus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(7, ChronoUnit.DAYS))
                .isActive(true)
                .timesUsed(5)
                .totalUsageLimit(100)
                .usageLimitPerUser(1)
                .build();
    }

    @Test
    @DisplayName("Should apply percentage discount capped by maxDiscountCap")
    void testPercentageDiscountCapped() {
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentCoupon));
        when(couponUsageRepository.countByCouponIdAndCustomerId(percentCoupon.getId(), customerId)).thenReturn(0L);

        // Subtotal = Rs. 4,000 (400000 paisa). 20% = Rs. 800 (80000 paisa), but capped at Rs. 500 (50000 paisa)
        long discount = promotionService.validateAndCalculateDiscount("SAVE20", customerId, 400000L);

        assertEquals(50000L, discount);
    }

    @Test
    @DisplayName("Should apply flat discount correctly")
    void testFlatDiscount() {
        when(couponRepository.findByCodeIgnoreCase("FLAT300")).thenReturn(Optional.of(flatCoupon));
        when(couponUsageRepository.countByCouponIdAndCustomerId(flatCoupon.getId(), customerId)).thenReturn(0L);

        long discount = promotionService.validateAndCalculateDiscount("FLAT300", customerId, 150000L);

        assertEquals(30000L, discount);
    }

    @Test
    @DisplayName("Should reject coupon when minimum order value is not met")
    void testMinOrderValueValidation() {
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentCoupon));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                promotionService.validateAndCalculateDiscount("SAVE20", customerId, 50000L) // Rs. 500 < Rs. 1,000 min
        );

        assertTrue(ex.getMessage().contains("Order subtotal must be at least"));
    }

    @Test
    @DisplayName("Should reject expired coupons")
    void testExpiredCoupon() {
        percentCoupon.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentCoupon));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                promotionService.validateAndCalculateDiscount("SAVE20", customerId, 200000L)
        );

        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Should return valid preview response for coupon validation")
    void testValidateCouponPreview() {
        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(percentCoupon));

        CouponValidationResponse preview = promotionService.validateCouponPreview("SAVE20", 200000L);

        assertTrue(preview.isValid());
        // 20% of 200,000 = 40,000 (which is <= 50,000 cap)
        assertEquals(40000L, preview.getDiscountPaisa());
        assertEquals(160000L, preview.getFinalTotalPaisa());
    }
}
