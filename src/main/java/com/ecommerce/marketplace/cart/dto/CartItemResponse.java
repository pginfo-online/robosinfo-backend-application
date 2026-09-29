package com.ecommerce.marketplace.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private UUID id;
    private UUID variantId;
    private UUID sellerListingId;
    private String productTitle;
    private String productSlug;
    private String sku;
    private String variantAttributes;
    private String imageUrl;
    private Long unitPricePaisa;
    private Long mrpPaisa;
    private Integer qty;
    private Long subtotalPaisa;
    private Boolean inStock;
    private Integer availableQty;
}
