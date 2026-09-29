package com.ecommerce.marketplace.returns.repository;

import com.ecommerce.marketplace.returns.model.ReturnInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnInspectionRepository extends JpaRepository<ReturnInspection, UUID> {
    Optional<ReturnInspection> findByReturnRequestId(UUID returnRequestId);
}
