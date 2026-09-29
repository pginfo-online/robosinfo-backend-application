package com.ecommerce.marketplace.audit.repository;

import com.ecommerce.marketplace.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, UUID targetId);

    Page<AuditLog> findByCorrelationIdOrderByCreatedAtDesc(UUID correlationId, Pageable pageable);
}
