package com.ecommerce.marketplace.warehouse.repository;

import com.ecommerce.marketplace.warehouse.model.GoodsReceiptNote;
import com.ecommerce.marketplace.warehouse.model.GrnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, UUID> {
    Optional<GoodsReceiptNote> findByGrnNumber(String grnNumber);
    Page<GoodsReceiptNote> findByWarehouseId(UUID warehouseId, Pageable pageable);
    Page<GoodsReceiptNote> findByWarehouseIdAndStatus(UUID warehouseId, GrnStatus status, Pageable pageable);
}
