package com.ecommerce.marketplace.search.service;

import com.ecommerce.marketplace.catalog.model.*;
import com.ecommerce.marketplace.catalog.repository.*;
import com.ecommerce.marketplace.review.repository.ReviewRepository;
import com.ecommerce.marketplace.search.dto.*;
import com.ecommerce.marketplace.search.model.SearchSortBy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SellerListingRepository listingRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public SearchResponse search(String query,
                                 UUID categoryId,
                                 UUID brandId,
                                 Long minPricePaisa,
                                 Long maxPricePaisa,
                                 Double minRating,
                                 Boolean inStockOnly,
                                 SearchSortBy sortBy,
                                 int page,
                                 int size) {

        List<Product> allApproved = productRepository.findByStatus(ProductStatus.APPROVED, null).getContent();

        // 1. Text filter
        String cleanQuery = (query != null && !query.isBlank()) ? query.toLowerCase().trim() : null;
        List<Product> matched = allApproved.stream()
            .filter(p -> {
                if (cleanQuery == null) return true;
                String title = p.getTitle().toLowerCase();
                String desc = p.getDescription() != null ? p.getDescription().toLowerCase() : "";
                return title.contains(cleanQuery) || desc.contains(cleanQuery);
            })
            .collect(Collectors.toList());

        // 2. Category & Brand filters
        if (categoryId != null) {
            matched = matched.stream().filter(p -> p.getCategoryId().equals(categoryId)).collect(Collectors.toList());
        }
        if (brandId != null) {
            matched = matched.stream().filter(p -> p.getBrandId() != null && p.getBrandId().equals(brandId)).collect(Collectors.toList());
        }

        // 3. Build product cards with Buy Box pricing & ratings
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
            .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
        Map<UUID, String> brandNames = brandRepository.findAll().stream()
            .collect(Collectors.toMap(Brand::getId, Brand::getName, (a, b) -> a));

        List<SearchProductCard> cards = new ArrayList<>();
        for (Product p : matched) {
            Long minPrice = null;
            Long mrpPrice = null;
            boolean hasStock = false;

            // Find lowest price across variants (Buy Box)
            for (ProductVariant v : p.getVariants()) {
                Optional<SellerListing> lowest = listingRepository
                    .findFirstByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(v.getId());
                if (lowest.isPresent()) {
                    hasStock = true;
                    if (minPrice == null || lowest.get().getSellingPricePaisa() < minPrice) {
                        minPrice = lowest.get().getSellingPricePaisa();
                        mrpPrice = lowest.get().getMrpPaisa();
                    }
                }
            }

            // Image
            String imageUrl = p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl();

            // Ratings
            Double avgRating = reviewRepository.getAverageRatingByProductId(p.getId());
            Long reviewCount = reviewRepository.countByProductId(p.getId());

            SearchProductCard card = SearchProductCard.builder()
                .id(p.getId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .categoryId(p.getCategoryId())
                .categoryName(categoryNames.get(p.getCategoryId()))
                .brandId(p.getBrandId())
                .brandName(brandNames.get(p.getBrandId()))
                .minPricePaisa(minPrice != null ? minPrice : 0L)
                .mrpPaisa(mrpPrice != null ? mrpPrice : 0L)
                .imageUrl(imageUrl)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .totalReviews(reviewCount != null ? reviewCount : 0L)
                .inStock(hasStock)
                .createdAt(p.getCreatedAt())
                .build();

            // Apply price and rating filters
            boolean pricePass = true;
            if (minPricePaisa != null && card.getMinPricePaisa() < minPricePaisa) pricePass = false;
            if (maxPricePaisa != null && card.getMinPricePaisa() > maxPricePaisa) pricePass = false;

            boolean ratingPass = minRating == null || card.getAverageRating() >= minRating;
            boolean stockPass = !Boolean.TRUE.equals(inStockOnly) || card.getInStock();

            if (pricePass && ratingPass && stockPass) {
                cards.add(card);
            }
        }

        // 4. Compute dynamic facet counts based on matched cards
        Map<UUID, Long> catCounts = cards.stream()
            .collect(Collectors.groupingBy(SearchProductCard::getCategoryId, Collectors.counting()));
        List<FacetCount> categoryFacets = catCounts.entrySet().stream()
            .map(e -> new FacetCount(e.getKey(), categoryNames.getOrDefault(e.getKey(), "Unknown"), e.getValue()))
            .collect(Collectors.toList());

        Map<UUID, Long> brCounts = cards.stream()
            .filter(c -> c.getBrandId() != null)
            .collect(Collectors.groupingBy(SearchProductCard::getBrandId, Collectors.counting()));
        List<FacetCount> brandFacets = brCounts.entrySet().stream()
            .map(e -> new FacetCount(e.getKey(), brandNames.getOrDefault(e.getKey(), "Unknown"), e.getValue()))
            .collect(Collectors.toList());

        Long globalMin = cards.stream().mapToLong(SearchProductCard::getMinPricePaisa).min().orElse(0L);
        Long globalMax = cards.stream().mapToLong(SearchProductCard::getMinPricePaisa).max().orElse(0L);

        SearchFacets facets = SearchFacets.builder()
            .categories(categoryFacets)
            .brands(brandFacets)
            .minPricePaisa(globalMin)
            .maxPricePaisa(globalMax)
            .build();

        // 5. Sort
        if (sortBy != null) {
            switch (sortBy) {
                case PRICE_LOW_HIGH -> cards.sort(Comparator.comparing(SearchProductCard::getMinPricePaisa));
                case PRICE_HIGH_LOW -> cards.sort(Comparator.comparing(SearchProductCard::getMinPricePaisa).reversed());
                case NEWEST -> cards.sort(Comparator.comparing(SearchProductCard::getCreatedAt).reversed());
                case RATING -> cards.sort(Comparator.comparing(SearchProductCard::getAverageRating).reversed());
                default -> {}
            }
        }

        // 6. Pagination
        int total = cards.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<SearchProductCard> pageContent = cards.subList(fromIndex, toIndex);
        int totalPages = (int) Math.ceil((double) total / (double) size);

        return SearchResponse.builder()
            .products(pageContent)
            .facets(facets)
            .totalResults((long) total)
            .page(page)
            .pageSize(size)
            .totalPages(totalPages)
            .build();
    }

    @Transactional(readOnly = true)
    public SearchSuggestionResponse getSuggestions(String query) {
        if (query == null || query.isBlank()) {
            return SearchSuggestionResponse.builder()
                .query("")
                .suggestions(Collections.emptyList())
                .items(Collections.emptyList())
                .build();
        }

        String prefix = query.toLowerCase().trim();
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
            .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        List<Product> matched = productRepository.findByStatus(ProductStatus.APPROVED, null).getContent()
            .stream()
            .filter(p -> p.getTitle().toLowerCase().contains(prefix) ||
                         (p.getDescription() != null && p.getDescription().toLowerCase().contains(prefix)))
            .limit(10)
            .collect(Collectors.toList());

        List<String> suggestions = matched.stream()
            .map(Product::getTitle)
            .distinct()
            .collect(Collectors.toList());

        List<SearchSuggestionItem> items = matched.stream().map(p -> {
            String imgUrl = p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl();
            Long minPrice = null;
            for (ProductVariant v : p.getVariants()) {
                Optional<SellerListing> lowest = listingRepository
                    .findFirstByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(v.getId());
                if (lowest.isPresent() && (minPrice == null || lowest.get().getSellingPricePaisa() < minPrice)) {
                    minPrice = lowest.get().getSellingPricePaisa();
                }
            }

            return SearchSuggestionItem.builder()
                .id(p.getId())
                .text(p.getTitle())
                .categoryName(categoryNames.get(p.getCategoryId()))
                .slug(p.getSlug())
                .imageUrl(imgUrl)
                .minPricePaisa(minPrice)
                .build();
        }).collect(Collectors.toList());

        return SearchSuggestionResponse.builder()
            .query(query)
            .suggestions(suggestions)
            .items(items)
            .build();
    }
}
