package com.ecommerce.marketplace.order.dto;

import com.ecommerce.marketplace.order.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private OrderStatus status;
    private String shippingAddressSnapshot;
    private Long subtotalPaisa;
    private Long discountPaisa;
    private Long taxPaisa;
    private Long shippingPaisa;
    private Long totalPaisa;
    private String couponCode;
    private String notes;
    private List<OrderItemResponse> items;
    private Instant createdAt;
}
