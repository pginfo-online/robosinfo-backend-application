package com.ecommerce.marketplace.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponse {

    private UUID customerId;
    private Integer totalItems;
    private List<WishlistItemResponse> items;
}
