package com.ecommerce.marketplace.catalog.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.marketplace.catalog.dto.CategoryRequest;
import com.ecommerce.marketplace.catalog.dto.CategoryResponse;
import com.ecommerce.marketplace.catalog.service.CategoryService;
import com.ecommerce.marketplace.common.dto.ApiResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Category Catalog", description = "Endpoints for viewing and managing product categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping
    @Operation(summary = "List all categories (hierarchical)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success("Category created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an existing category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated", response));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Toggle category active/inactive status (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleCategoryActive(@PathVariable UUID id) {
        CategoryResponse response = categoryService.toggleCategoryActive(id);
        return ResponseEntity.ok(ApiResponse.success("Category status toggled", response));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Upload or replace the banner image for a category (Admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> uploadCategoryImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Cannot upload empty file", "EMPTY_FILE");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalFilename.substring(dotIdx + 1).toLowerCase();
        }

        String imageUrl;

        // Try Cloudinary first if configured
        if (cloudName != null && !cloudName.isBlank() && !cloudName.equalsIgnoreCase("mock")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                        "folder", "categories",
                        "resource_type", "auto"
                    )
                );
                imageUrl = (String) result.get("secure_url");
                log.info("Category {} image uploaded to Cloudinary: {}", id, imageUrl);
            } catch (Exception e) {
                log.warn("Cloudinary upload failed for category {}, falling back to local storage: {}", id, e.getMessage());
                imageUrl = saveLocally(file, extension, id);
            }
        } else {
            imageUrl = saveLocally(file, extension, id);
        }

        CategoryResponse response = categoryService.updateCategoryImage(id, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Category image uploaded", response));
    }

    private String saveLocally(MultipartFile file, String extension, UUID categoryId) {
        try {
            Path targetDir = Paths.get(uploadDir, "categories");
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            String filename = categoryId + (extension.isEmpty() ? "" : "." + extension);
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/categories/" + filename;
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to store category image: " + e.getMessage(), "UPLOAD_FAILED");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Soft-delete a category (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}
