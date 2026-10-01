package com.ecommerce.marketplace.catalog.service;

import com.ecommerce.marketplace.catalog.dto.CategoryRequest;
import com.ecommerce.marketplace.catalog.dto.CategoryResponse;
import com.ecommerce.marketplace.catalog.model.Category;
import com.ecommerce.marketplace.catalog.repository.CategoryRepository;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessRuleException("Category slug already exists", "SLUG_ALREADY_EXISTS");
        }

        int level = 0;
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Category", request.getParentId().toString()));
            level = parent.getLevel() + 1;
        }

        Category category = Category.builder()
            .name(request.getName())
            .slug(slug)
            .description(request.getDescription())
            .parentId(request.getParentId())
            .level(level)
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
            .imageUrl(request.getImageUrl())
            .commissionRatePercent(request.getCommissionRatePercent() != null
                ? request.getCommissionRatePercent() : BigDecimal.TEN)
            .attributesTemplate(request.getAttributesTemplate())
            .returnWindowDays(request.getReturnWindowDays() != null ? request.getReturnWindowDays() : 7)
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        category = categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(this::toCategoryResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        return categoryRepository.findById(id)
            .map(this::toCategoryResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
            .map(this::toCategoryResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

        // Validate slug uniqueness if changed
        String newSlug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());
        if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
            throw new BusinessRuleException("Category slug already exists", "SLUG_ALREADY_EXISTS");
        }

        // Recalculate level if parent changed
        int level = 0;
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessRuleException("Category cannot be its own parent", "INVALID_PARENT");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Category", request.getParentId().toString()));
            level = parent.getLevel() + 1;
        }

        category.setName(request.getName());
        category.setSlug(newSlug);
        category.setDescription(request.getDescription());
        category.setParentId(request.getParentId());
        category.setLevel(level);
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        if (request.getCommissionRatePercent() != null) category.setCommissionRatePercent(request.getCommissionRatePercent());
        if (request.getAttributesTemplate() != null) category.setAttributesTemplate(request.getAttributesTemplate());
        if (request.getReturnWindowDays() != null) category.setReturnWindowDays(request.getReturnWindowDays());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());

        category = categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse toggleCategoryActive(UUID id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
        category.setIsActive(!category.getIsActive());
        category = categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

        // Check if category has children
        List<Category> children = categoryRepository.findByParentIdOrderByDisplayOrderAsc(id);
        if (!children.isEmpty()) {
            throw new BusinessRuleException("Cannot delete category with subcategories. Remove children first.", "HAS_CHILDREN");
        }

        category.setIsActive(false);
        categoryRepository.save(category);
    }

    // Called by CategoryController after image upload succeeds
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategoryImage(UUID id, String imageUrl) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));
        category.setImageUrl(imageUrl);
        return toCategoryResponse(categoryRepository.save(category));
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .description(category.getDescription())
            .parentId(category.getParentId())
            .level(category.getLevel())
            .displayOrder(category.getDisplayOrder())
            .imageUrl(category.getImageUrl())
            .isActive(category.getIsActive())
            .commissionRatePercent(category.getCommissionRatePercent())
            .attributesTemplate(category.getAttributesTemplate())
            .returnWindowDays(category.getReturnWindowDays())
            .createdAt(category.getCreatedAt())
            .build();
    }

    private String generateSlug(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
