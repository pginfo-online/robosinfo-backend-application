package com.ecommerce.marketplace.catalog.repository;

import com.ecommerce.marketplace.catalog.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
