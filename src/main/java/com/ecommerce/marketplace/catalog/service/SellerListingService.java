package com.ecommerce.marketplace.catalog.service;

import com.ecommerce.marketplace.catalog.dto.SellerListingDetailResponse;
import com.ecommerce.marketplace.catalog.dto.SellerListingRequest;
import com.ecommerce.marketplace.catalog.dto.SellerListingResponse;
import com.ecommerce.marketplace.catalog.dto.UpdateListingRequest;
import com.ecommerce.marketplace.catalog.model.*;
import com.ecommerce.marketplace.catalog.repository.*;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.inventory.model.Inventory;
import com.ecommerce.marketplace.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerListingService {

    private final SellerListingRepository listingRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    public SellerListingResponse createOrUpdateListing(UUID sellerId, SellerListingRequest request) {
        if (!variantRepository.existsById(request.getVariantId())) {
            throw new ResourceNotFoundException("Product Variant", request.getVariantId().toString());
        }

        if (request.getSellingPricePaisa() > request.getMrpPaisa()) {
            throw new BusinessRuleException("Selling price cannot exceed Maximum Retail Price (MRP)", "PRICE_EXCEEDS_MRP");
        }

        SellerListing listing = listingRepository.findBySellerIdAndVariantId(sellerId, request.getVariantId())
            .orElseGet(() -> SellerListing.builder()
                .sellerId(sellerId)
                .variantId(request.getVariantId())
                .build());

        listing.setMrpPaisa(request.getMrpPaisa());
        listing.setSellingPricePaisa(request.getSellingPricePaisa());
        listing.setCondition(request.getCondition());
        listing.setFulfillmentType(request.getFulfillmentType());
        listing.setIsActive(true);

        listing = listingRepository.save(listing);
        return toListingResponse(listing);
    }

    @Transactional(readOnly = true)
    public List<SellerListingResponse> getSellerListings(UUID sellerId) {
        return listingRepository.findBySellerId(sellerId).stream()
            .map(this::toListingResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerListingDetailResponse> getMyListingsEnriched(
            UUID sellerId, Pageable pageable, String search, Boolean isActive) {

        Page<SellerListing> page;
        if (isActive != null) {
            page = listingRepository.findBySellerIdAndIsActive(sellerId, isActive, pageable);
        } else {
            page = listingRepository.findBySellerId(sellerId, pageable);
        }

        List<SellerListingDetailResponse> enrichedList = page.getContent().stream()
            .map(this::toListingDetailResponse)
            .filter(item -> {
                if (search == null || search.isBlank()) {
                    return true;
                }
                String q = search.toLowerCase().trim();
                return (item.getProductTitle() != null && item.getProductTitle().toLowerCase().contains(q))
                    || (item.getSku() != null && item.getSku().toLowerCase().contains(q))
                    || (item.getBrandName() != null && item.getBrandName().toLowerCase().contains(q));
            })
            .collect(Collectors.toList());

        return new PageResponse<>(enrichedList, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public SellerListingDetailResponse getListingDetail(UUID sellerId, UUID listingId) {
        SellerListing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId.toString()));

        if (!listing.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("You do not have permission to view this listing", "ACCESS_DENIED");
        }

        return toListingDetailResponse(listing);
    }

    @Transactional
    public SellerListingDetailResponse updateListing(UUID sellerId, UUID listingId, UpdateListingRequest request) {
        SellerListing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId.toString()));

        if (!listing.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("You do not have permission to modify this listing", "ACCESS_DENIED");
        }

        if (request.getMrpPaisa() != null) {
            listing.setMrpPaisa(request.getMrpPaisa());
        }
        if (request.getSellingPricePaisa() != null) {
            if (listing.getMrpPaisa() != null && request.getSellingPricePaisa() > listing.getMrpPaisa()) {
                throw new BusinessRuleException("Selling price cannot exceed Maximum Retail Price (MRP)", "PRICE_EXCEEDS_MRP");
            }
            listing.setSellingPricePaisa(request.getSellingPricePaisa());
        }
        if (request.getIsActive() != null) {
            listing.setIsActive(request.getIsActive());
        }
        if (request.getCondition() != null) {
            listing.setCondition(request.getCondition());
        }
        if (request.getFulfillmentType() != null) {
            listing.setFulfillmentType(request.getFulfillmentType());
        }

        listing = listingRepository.save(listing);
        return toListingDetailResponse(listing);
    }

    @Transactional
    public void deleteListing(UUID sellerId, UUID listingId) {
        SellerListing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ResourceNotFoundException("Listing", listingId.toString()));

        if (!listing.getSellerId().equals(sellerId)) {
            throw new BusinessRuleException("You do not have permission to delete this listing", "ACCESS_DENIED");
        }

        listing.setIsActive(false);
        listingRepository.save(listing);
    }

    @Transactional(readOnly = true)
    public List<SellerListingResponse> getVariantListings(UUID variantId) {
        return listingRepository.findByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(variantId).stream()
            .map(this::toListingResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<SellerListingResponse> getBuyBox(UUID variantId) {
        return listingRepository.findFirstByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(variantId)
            .map(this::toListingResponse);
    }

    public SellerListingDetailResponse toListingDetailResponse(SellerListing listing) {
        ProductVariant variant = variantRepository.findById(listing.getVariantId()).orElse(null);
        Product product = variant != null ? variant.getProduct() : null;

        String productTitle = product != null ? product.getTitle() : "Unknown Product";
        UUID productId = product != null ? product.getId() : null;
        String sku = variant != null ? variant.getSku() : "";

        String categoryName = null;
        if (product != null && product.getCategoryId() != null) {
            categoryName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName).orElse(null);
        }

        String brandName = null;
        if (product != null && product.getBrandId() != null) {
            brandName = brandRepository.findById(product.getBrandId())
                .map(Brand::getName).orElse(null);
        }

        String primaryImageUrl = null;
        if (productId != null) {
            primaryImageUrl = imageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElseGet(() -> imageRepository.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                    .map(ProductImage::getUrl)
                    .findFirst()
                    .orElse(null));
        }

        Integer availableStock = inventoryRepository.findByVariantIdAndSellerId(listing.getVariantId(), listing.getSellerId())
            .map(Inventory::getAvailableQty)
            .orElse(0);

        return SellerListingDetailResponse.builder()
            .id(listing.getId())
            .sellerId(listing.getSellerId())
            .variantId(listing.getVariantId())
            .productId(productId)
            .productTitle(productTitle)
            .sku(sku)
            .brandName(brandName)
            .categoryName(categoryName)
            .primaryImageUrl(primaryImageUrl)
            .mrpPaisa(listing.getMrpPaisa())
            .sellingPricePaisa(listing.getSellingPricePaisa())
            .availableStock(availableStock)
            .isActive(listing.getIsActive())
            .condition(listing.getCondition())
            .fulfillmentType(listing.getFulfillmentType())
            .createdAt(listing.getCreatedAt())
            .updatedAt(listing.getUpdatedAt())
            .build();
    }

    public SellerListingResponse toListingResponse(SellerListing listing) {
        return SellerListingResponse.builder()
            .id(listing.getId())
            .sellerId(listing.getSellerId())
            .variantId(listing.getVariantId())
            .mrpPaisa(listing.getMrpPaisa())
            .sellingPricePaisa(listing.getSellingPricePaisa())
            .isActive(listing.getIsActive())
            .condition(listing.getCondition())
            .fulfillmentType(listing.getFulfillmentType())
            .build();
    }
}
