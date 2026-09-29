package com.ecommerce.marketplace.catalog.repository;

import com.ecommerce.marketplace.catalog.model.Product;
import com.ecommerce.marketplace.catalog.model.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status, Pageable pageable);

    Page<Product> findByBrandIdAndStatus(UUID brandId, ProductStatus status, Pageable pageable);

    Page<Product> findByCreatedBy(UUID createdBy, Pageable pageable);

    boolean existsBySlug(String slug);
}
