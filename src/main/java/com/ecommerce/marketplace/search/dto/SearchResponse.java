package com.ecommerce.marketplace.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    private List<SearchProductCard> products;
    private SearchFacets facets;
    private Long totalResults;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
}
