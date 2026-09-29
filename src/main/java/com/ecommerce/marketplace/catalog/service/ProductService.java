package com.ecommerce.marketplace.catalog.service;

import com.ecommerce.marketplace.catalog.dto.*;
import com.ecommerce.marketplace.catalog.model.*;
import com.ecommerce.marketplace.catalog.repository.*;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.marketplace.review.repository.ReviewRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final SellerListingService sellerListingService;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ProductResponse createProduct(UUID createdBy, ProductRequest request) {
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ResourceNotFoundException("Category", request.getCategoryId().toString());
        }

        if (request.getBrandId() != null && !brandRepository.existsById(request.getBrandId())) {
            throw new ResourceNotFoundException("Brand", request.getBrandId().toString());
        }

        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getTitle()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        if (productRepository.existsBySlug(slug)) {
            throw new BusinessRuleException("Product slug already exists", "SLUG_ALREADY_EXISTS");
        }

        Product product = Product.builder()
            .title(request.getTitle())
            .slug(slug)
            .description(request.getDescription())
            .categoryId(request.getCategoryId())
            .brandId(request.getBrandId())
            .attributes(request.getAttributes())
            .status(ProductStatus.APPROVED) // Auto-approved in v1
            .createdBy(createdBy)
            .build();

        product = productRepository.save(product);

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            for (VariantRequest vr : request.getVariants()) {
                if (variantRepository.existsBySku(vr.getSku())) {
                    throw new BusinessRuleException("SKU already exists: " + vr.getSku(), "SKU_ALREADY_EXISTS");
                }
                ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(vr.getSku())
                    .variantAttributes(vr.getVariantAttributes())
                    .weightGrams(vr.getWeightGrams())
                    .dimensionsCm(vr.getDimensionsCm())
                    .isActive(true)
                    .build();
                product.getVariants().add(variant);
            }
            product = productRepository.save(product);
        }

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID userId, UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            product.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            if (!categoryRepository.existsById(request.getCategoryId())) {
                throw new ResourceNotFoundException("Category", request.getCategoryId().toString());
            }
            product.setCategoryId(request.getCategoryId());
        }
        if (request.getBrandId() != null) {
            if (!brandRepository.existsById(request.getBrandId())) {
                throw new ResourceNotFoundException("Brand", request.getBrandId().toString());
            }
            product.setBrandId(request.getBrandId());
        }
        if (request.getAttributes() != null) {
            product.setAttributes(request.getAttributes());
        }

        product = productRepository.save(product);
        return toProductResponse(product);
    }

    @Transactional
    public void deleteProduct(UUID userId, UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        product.setStatus(ProductStatus.SUSPENDED);
        productRepository.save(product);
    }


    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product", slug));
        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(UUID categoryId, UUID brandId, Pageable pageable) {
        Page<Product> page;
        if (categoryId != null) {
            page = productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.APPROVED, pageable);
        } else if (brandId != null) {
            page = productRepository.findByBrandIdAndStatus(brandId, ProductStatus.APPROVED, pageable);
        } else {
            page = productRepository.findByStatus(ProductStatus.APPROVED, pageable);
        }

        List<ProductResponse> data = page.getContent().stream()
            .map(this::toProductResponse)
            .collect(Collectors.toList());

        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(UUID productId, int limit) {
        Product current = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        return productRepository.findByCategoryIdAndStatus(current.getCategoryId(), ProductStatus.APPROVED, null).getContent()
            .stream()
            .filter(p -> !p.getId().equals(productId))
            .limit(limit > 0 ? limit : 8)
            .map(this::toProductResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        return productRepository.findByStatus(ProductStatus.APPROVED, null).getContent()
            .stream()
            .limit(limit > 0 ? limit : 12)
            .map(this::toProductResponse)
            .collect(Collectors.toList());
    }

    private ProductResponse toProductResponse(Product product) {
        String categoryName = categoryRepository.findById(product.getCategoryId())
            .map(Category::getName).orElse(null);
        String brandName = product.getBrandId() != null
            ? brandRepository.findById(product.getBrandId()).map(Brand::getName).orElse(null)
            : null;

        List<VariantResponse> variants = product.getVariants().stream()
            .map(v -> VariantResponse.builder()
                .id(v.getId())
                .productId(product.getId())
                .sku(v.getSku())
                .variantAttributes(v.getVariantAttributes())
                .weightGrams(v.getWeightGrams())
                .dimensionsCm(v.getDimensionsCm())
                .isActive(v.getIsActive())
                .buyBoxListing(sellerListingService.getBuyBox(v.getId()).orElse(null))
                .build())
            .collect(Collectors.toList());

        List<String> imageUrls = imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId()).stream()
            .map(ProductImage::getUrl)
            .collect(Collectors.toList());

        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        Long reviewCount = reviewRepository.countByProductId(product.getId());

        return ProductResponse.builder()
            .id(product.getId())
            .title(product.getTitle())
            .slug(product.getSlug())
            .description(product.getDescription())
            .categoryId(product.getCategoryId())
            .categoryName(categoryName)
            .brandId(product.getBrandId())
            .brandName(brandName)
            .attributes(product.getAttributes())
            .status(product.getStatus())
            .createdBy(product.getCreatedBy())
            .variants(variants)
            .imageUrls(imageUrls)
            .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
            .totalReviews(reviewCount != null ? reviewCount : 0L)
            .createdAt(product.getCreatedAt())
            .build();
    }

    private String generateSlug(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
