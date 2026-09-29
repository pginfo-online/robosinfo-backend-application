package com.ecommerce.marketplace.catalog.controller;

import com.ecommerce.marketplace.catalog.dto.ProductRequest;
import com.ecommerce.marketplace.catalog.dto.ProductResponse;
import com.ecommerce.marketplace.catalog.service.ProductService;
import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.identity.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Endpoints for browsing, searching, and creating products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Browse products with category/brand filtering and pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<ProductResponse> products = productService.getProducts(categoryId, brandId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID (including variants and Buy Box pricing)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products for homepage showcase")
    public ResponseEntity<ApiResponse<java.util.List<ProductResponse>>> getFeaturedProducts(
            @RequestParam(required = false, defaultValue = "12") int limit) {
        java.util.List<ProductResponse> products = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Get related products in same category")
    public ResponseEntity<ApiResponse<java.util.List<ProductResponse>>> getRelatedProducts(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "8") int limit) {
        java.util.List<ProductResponse> products = productService.getRelatedProducts(id, limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product details by URL slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        ProductResponse product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new product with variants (Sellers & Admins)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Product created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an existing product (Sellers & Admins)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Archive/deactivate a product (Sellers & Admins)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        productService.deleteProduct(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}

