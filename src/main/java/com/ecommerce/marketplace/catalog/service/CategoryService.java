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
            .parentId(request.getParentId())
            .level(level)
            .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
            .imageUrl(request.getImageUrl())
            .attributesTemplate(request.getAttributesTemplate())
            .returnWindowDays(request.getReturnWindowDays() != null ? request.getReturnWindowDays() : 7)
            .isActive(true)
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

    private CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .parentId(category.getParentId())
            .level(category.getLevel())
            .displayOrder(category.getDisplayOrder())
            .imageUrl(category.getImageUrl())
            .isActive(category.getIsActive())
            .attributesTemplate(category.getAttributesTemplate())
            .returnWindowDays(category.getReturnWindowDays())
            .createdAt(category.getCreatedAt())
            .build();
    }

    private String generateSlug(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
