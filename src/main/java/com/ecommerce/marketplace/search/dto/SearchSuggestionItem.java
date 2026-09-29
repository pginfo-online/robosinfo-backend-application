package com.ecommerce.marketplace.search.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchSuggestionItem {
    private UUID id;
    private String text;
    private String categoryName;
    private String slug;
    private String imageUrl;
    private Long minPricePaisa;
}
