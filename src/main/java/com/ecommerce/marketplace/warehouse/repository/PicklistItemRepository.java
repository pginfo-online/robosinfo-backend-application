package com.ecommerce.marketplace.warehouse.repository;

import com.ecommerce.marketplace.warehouse.model.PicklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PicklistItemRepository extends JpaRepository<PicklistItem, UUID> {
    List<PicklistItem> findByPicklistId(UUID picklistId);
}
