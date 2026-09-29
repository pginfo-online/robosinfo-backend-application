package com.ecommerce.marketplace.order.dto;

import com.ecommerce.marketplace.order.model.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerOrderResponse {

    private UUID orderId;
    private UUID orderItemId;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String city;
    private String state;
    private String pincode;

    private UUID variantId;
    private String productTitle;
    private String sku;
    private String imageUrl;
    private Integer qty;
    private Long unitPricePaisa;
    private Long totalPaisa;
    private Long taxPaisa;

    private OrderItemStatus status;
    private String orderStatus;
    private String carrierName;
    private String trackingNumber;

    private Instant createdAt;
}
