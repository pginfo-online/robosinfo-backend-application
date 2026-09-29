package com.ecommerce.marketplace.order.repository;

import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    Page<OrderItem> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<OrderItem> findBySellerIdAndStatus(UUID sellerId, OrderItemStatus status, Pageable pageable);
}
