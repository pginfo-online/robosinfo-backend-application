package com.ecommerce.marketplace.catalog.service;

import com.ecommerce.marketplace.catalog.dto.BrandRequest;
import com.ecommerce.marketplace.catalog.dto.BrandResponse;
import com.ecommerce.marketplace.catalog.model.Brand;
import com.ecommerce.marketplace.catalog.repository.BrandRepository;
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
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse createBrand(BrandRequest request) {
        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());
        if (brandRepository.existsBySlug(slug)) {
            throw new BusinessRuleException("Brand slug already exists", "SLUG_ALREADY_EXISTS");
        }

        Brand brand = Brand.builder()
            .name(request.getName())
            .slug(slug)
            .logoUrl(request.getLogoUrl())
            .isActive(true)
            .build();

        brand = brandRepository.save(brand);
        return toBrandResponse(brand);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "brands", key = "'all'")
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
            .map(this::toBrandResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(UUID id) {
        return brandRepository.findById(id)
            .map(this::toBrandResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Brand", id.toString()));
    }

    private BrandResponse toBrandResponse(Brand brand) {
        return BrandResponse.builder()
            .id(brand.getId())
            .name(brand.getName())
            .slug(brand.getSlug())
            .logoUrl(brand.getLogoUrl())
            .isActive(brand.getIsActive())
            .createdAt(brand.getCreatedAt())
            .build();
    }

    private String generateSlug(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
