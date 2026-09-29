package com.ecommerce.marketplace.promotion.service;

import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.promotion.dto.*;
import com.ecommerce.marketplace.promotion.model.Coupon;
import com.ecommerce.marketplace.promotion.model.CouponUsage;
import com.ecommerce.marketplace.promotion.model.DiscountType;
import com.ecommerce.marketplace.promotion.repository.CouponRepository;
import com.ecommerce.marketplace.promotion.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Transactional(readOnly = true)
    public long validateAndCalculateDiscount(String code, UUID customerId, long subtotalPaisa) {
        if (code == null || code.isBlank()) {
            return 0L;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
            .orElseThrow(() -> new BusinessRuleException("Invalid coupon code: " + code, "INVALID_COUPON"));

        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new BusinessRuleException("Coupon " + code + " is inactive", "COUPON_INACTIVE");
        }

        Instant now = Instant.now();
        if (now.isBefore(coupon.getStartDate()) || now.isAfter(coupon.getEndDate())) {
            throw new BusinessRuleException("Coupon " + code + " has expired or is not yet active", "COUPON_EXPIRED");
        }

        if (subtotalPaisa < coupon.getMinOrderValuePaisa()) {
            throw new BusinessRuleException(
                "Order subtotal must be at least Rs. " + (coupon.getMinOrderValuePaisa() / 100) + " to use this coupon",
                "MIN_ORDER_NOT_MET"
            );
        }

        if (coupon.getTimesUsed() >= coupon.getTotalUsageLimit()) {
            throw new BusinessRuleException("Coupon " + code + " usage limit reached", "COUPON_EXHAUSTED");
        }

        if (customerId != null) {
            long userUsage = couponUsageRepository.countByCouponIdAndCustomerId(coupon.getId(), customerId);
            if (userUsage >= coupon.getUsageLimitPerUser()) {
                throw new BusinessRuleException(
                    "You have reached the maximum allowed uses for coupon " + code,
                    "COUPON_USER_LIMIT_REACHED"
                );
            }
        }

        return calculateDiscountAmount(coupon, subtotalPaisa);
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validateCouponPreview(String code, long cartTotalPaisa) {
        try {
            long discount = validateAndCalculateDiscount(code, null, cartTotalPaisa);
            Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim()).orElseThrow();
            long finalTotal = Math.max(0, cartTotalPaisa - discount);

            return CouponValidationResponse.builder()
                .valid(true)
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountPaisa(discount)
                .finalTotalPaisa(finalTotal)
                .message("Coupon applied successfully")
                .build();
        } catch (BusinessRuleException e) {
            return CouponValidationResponse.builder()
                .valid(false)
                .code(code)
                .discountPaisa(0L)
                .finalTotalPaisa(cartTotalPaisa)
                .message(e.getMessage())
                .build();
        }
    }

    @Transactional
    public void recordCouponUsage(String code, UUID customerId, UUID orderId, long discountPaisa) {
        if (code == null || code.isBlank() || discountPaisa <= 0) {
            return;
        }

        couponRepository.findByCodeIgnoreCase(code.trim()).ifPresent(coupon -> {
            CouponUsage usage = CouponUsage.builder()
                .couponId(coupon.getId())
                .customerId(customerId)
                .orderId(orderId)
                .discountPaisa(discountPaisa)
                .build();
            couponUsageRepository.save(usage);

            coupon.setTimesUsed(coupon.getTimesUsed() + 1);
            couponRepository.save(coupon);
            log.info("Recorded usage for coupon {} on order {}", coupon.getCode(), orderId);
        });
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        String code = request.getCode().toUpperCase().trim();
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new BusinessRuleException("Coupon code already exists", "DUPLICATE_COUPON_CODE");
        }

        Coupon coupon = Coupon.builder()
            .code(code)
            .description(request.getDescription())
            .discountType(request.getDiscountType())
            .discountValue(request.getDiscountValue())
            .minOrderValuePaisa(request.getMinOrderValuePaisa() != null ? request.getMinOrderValuePaisa() : 0L)
            .maxDiscountCapPaisa(request.getMaxDiscountCapPaisa())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .usageLimitPerUser(request.getUsageLimitPerUser() != null ? request.getUsageLimitPerUser() : 1)
            .totalUsageLimit(request.getTotalUsageLimit() != null ? request.getTotalUsageLimit() : 1000)
            .timesUsed(0)
            .isActive(true)
            .build();

        coupon = couponRepository.save(coupon);
        log.info("Created coupon {}", code);
        return toResponse(coupon);
    }

    @Transactional
    public CouponResponse toggleCouponStatus(UUID couponId, boolean isActive) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", couponId.toString()));
        coupon.setIsActive(isActive);
        coupon = couponRepository.save(coupon);
        return toResponse(coupon);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getActiveCoupons() {
        Instant now = Instant.now();
        return couponRepository.findAll().stream()
            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
            .filter(c -> (c.getStartDate() == null || !now.isBefore(c.getStartDate())) &&
                         (c.getEndDate() == null || !now.isAfter(c.getEndDate())))
            .filter(c -> c.getTotalUsageLimit() == null || c.getTimesUsed() < c.getTotalUsageLimit())
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> getAllCoupons(Pageable pageable) {
        Page<Coupon> page = couponRepository.findAll(pageable);
        List<CouponResponse> data = page.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    private long calculateDiscountAmount(Coupon coupon, long subtotalPaisa) {
        long discount;
        if (coupon.getDiscountType() == DiscountType.FLAT_AMOUNT) {
            discount = Math.min(coupon.getDiscountValue(), subtotalPaisa);
        } else {
            discount = (subtotalPaisa * coupon.getDiscountValue()) / 100;
            if (coupon.getMaxDiscountCapPaisa() != null && coupon.getMaxDiscountCapPaisa() > 0) {
                discount = Math.min(discount, coupon.getMaxDiscountCapPaisa());
            }
        }
        return discount;
    }

    public CouponResponse toResponse(Coupon c) {
        return CouponResponse.builder()
            .id(c.getId())
            .code(c.getCode())
            .description(c.getDescription())
            .discountType(c.getDiscountType())
            .discountValue(c.getDiscountValue())
            .minOrderValuePaisa(c.getMinOrderValuePaisa())
            .maxDiscountCapPaisa(c.getMaxDiscountCapPaisa())
            .startDate(c.getStartDate())
            .endDate(c.getEndDate())
            .usageLimitPerUser(c.getUsageLimitPerUser())
            .totalUsageLimit(c.getTotalUsageLimit())
            .timesUsed(c.getTimesUsed())
            .isActive(c.getIsActive())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
