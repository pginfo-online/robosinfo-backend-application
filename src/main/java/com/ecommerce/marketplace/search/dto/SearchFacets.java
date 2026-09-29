package com.ecommerce.marketplace.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchFacets {
    private List<FacetCount> categories;
    private List<FacetCount> brands;
    private Long minPricePaisa;
    private Long maxPricePaisa;
}
