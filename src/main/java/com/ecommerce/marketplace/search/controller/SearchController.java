package com.ecommerce.marketplace.search.controller;

import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.search.dto.SearchResponse;
import com.ecommerce.marketplace.search.dto.SearchSuggestionResponse;
import com.ecommerce.marketplace.search.model.SearchSortBy;
import com.ecommerce.marketplace.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Product Search & Discovery", description = "Endpoints for full-text catalog search, filtering, faceted aggregation, and autocomplete suggestions")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search product catalog with keyword, category, brand, price filters, and facets")
    public ResponseEntity<ApiResponse<SearchResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,
            @RequestParam(required = false, defaultValue = "RELEVANCE") SearchSortBy sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        SearchResponse response = searchService.search(
            q, categoryId, brandId, minPrice, maxPrice, minRating, inStockOnly, sortBy, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Get autocomplete suggestions for product search query prefix")
    public ResponseEntity<ApiResponse<SearchSuggestionResponse>> suggest(@RequestParam String q) {
        SearchSuggestionResponse response = searchService.getSuggestions(q);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
