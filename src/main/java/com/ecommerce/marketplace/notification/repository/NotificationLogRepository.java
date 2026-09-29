package com.ecommerce.marketplace.notification.repository;

import com.ecommerce.marketplace.notification.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    Page<NotificationLog> findByUserIdOrderBySentAtDesc(UUID userId, Pageable pageable);
}
