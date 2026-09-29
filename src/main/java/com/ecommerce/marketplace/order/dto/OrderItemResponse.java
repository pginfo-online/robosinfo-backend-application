package com.ecommerce.marketplace.order.dto;

import com.ecommerce.marketplace.order.model.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID id;
    private UUID variantId;
    private UUID sellerId;
    private UUID sellerListingId;
    private String productTitle;
    private String sku;
    private Integer qty;
    private Long unitPricePaisa;
    private Long mrpPaisa;
    private Long discountPaisa;
    private Long taxPaisa;
    private Long totalPaisa;
    private OrderItemStatus status;
}
