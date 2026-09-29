package com.ecommerce.marketplace.returns.repository;

import com.ecommerce.marketplace.returns.model.ReturnRequest;
import com.ecommerce.marketplace.returns.model.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    Optional<ReturnRequest> findByReturnNumber(String returnNumber);

    List<ReturnRequest> findByOrderItemId(UUID orderItemId);

    Optional<ReturnRequest> findByOrderItemIdAndStatusNot(UUID orderItemId, ReturnStatus status);

    Page<ReturnRequest> findByCustomerId(UUID customerId, Pageable pageable);

    Page<ReturnRequest> findBySellerId(UUID sellerId, Pageable pageable);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);
}
