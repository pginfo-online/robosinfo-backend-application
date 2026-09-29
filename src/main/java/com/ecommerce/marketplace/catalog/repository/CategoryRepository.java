package com.ecommerce.marketplace.catalog.repository;

import com.ecommerce.marketplace.catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIdOrderByDisplayOrderAsc(UUID parentId);

    List<Category> findByParentIdIsNullOrderByDisplayOrderAsc();

    boolean existsBySlug(String slug);
}
