package com.ecommerce.marketplace.search.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchSuggestionResponse {
    private String query;
    private List<String> suggestions;
    private List<SearchSuggestionItem> items;
}
