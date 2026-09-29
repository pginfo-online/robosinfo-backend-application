package com.ecommerce.marketplace.identity.service;

import com.ecommerce.marketplace.catalog.model.*;
import com.ecommerce.marketplace.catalog.repository.*;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.identity.dto.WishlistItemResponse;
import com.ecommerce.marketplace.identity.dto.WishlistResponse;
import com.ecommerce.marketplace.identity.model.WishlistItem;
import com.ecommerce.marketplace.identity.repository.WishlistRepository;
import com.ecommerce.marketplace.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SellerListingRepository listingRepository;
    private final ReviewRepository reviewRepository;
    private final ProductImageRepository imageRepository;

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(UUID customerId) {
        List<WishlistItem> items = wishlistRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        List<WishlistItemResponse> responses = new ArrayList<>();

        for (WishlistItem item : items) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isEmpty() || productOpt.get().getStatus() != ProductStatus.APPROVED) {
                continue;
            }

            Product product = productOpt.get();
            WishlistItemResponse res = buildWishlistItemResponse(item, product);
            responses.add(res);
        }

        return WishlistResponse.builder()
            .customerId(customerId)
            .totalItems(responses.size())
            .items(responses)
            .build();
    }

    @Transactional
    public WishlistItemResponse addToWishlist(UUID customerId, UUID productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        WishlistItem item = wishlistRepository.findByCustomerIdAndProductId(customerId, productId)
            .orElseGet(() -> wishlistRepository.save(WishlistItem.builder()
                .customerId(customerId)
                .productId(productId)
                .build()));

        return buildWishlistItemResponse(item, product);
    }

    @Transactional
    public void removeFromWishlist(UUID customerId, UUID productId) {
        wishlistRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getWishlistProductIds(UUID customerId) {
        return wishlistRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
            .map(WishlistItem::getProductId)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isInWishlist(UUID customerId, UUID productId) {
        return wishlistRepository.existsByCustomerIdAndProductId(customerId, productId);
    }

    private WishlistItemResponse buildWishlistItemResponse(WishlistItem item, Product product) {
        String categoryName = categoryRepository.findById(product.getCategoryId())
            .map(Category::getName).orElse(null);
        String brandName = product.getBrandId() != null
            ? brandRepository.findById(product.getBrandId()).map(Brand::getName).orElse(null)
            : null;

        Long minPrice = null;
        Long mrpPrice = null;
        boolean hasStock = false;

        for (ProductVariant v : product.getVariants()) {
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

        List<ProductImage> images = imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getUrl();

        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        Long reviewCount = reviewRepository.countByProductId(product.getId());

        return WishlistItemResponse.builder()
            .id(item.getId())
            .productId(product.getId())
            .title(product.getTitle())
            .slug(product.getSlug())
            .categoryName(categoryName)
            .brandName(brandName)
            .minPricePaisa(minPrice != null ? minPrice : 0L)
            .mrpPaisa(mrpPrice != null ? mrpPrice : 0L)
            .imageUrl(imageUrl)
            .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
            .totalReviews(reviewCount != null ? reviewCount : 0L)
            .inStock(hasStock)
            .addedAt(item.getCreatedAt())
            .build();
    }
}
