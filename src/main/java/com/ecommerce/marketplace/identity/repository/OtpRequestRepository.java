package com.ecommerce.marketplace.identity.repository;

import com.ecommerce.marketplace.identity.model.OtpPurpose;
import com.ecommerce.marketplace.identity.model.OtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRequestRepository extends JpaRepository<OtpRequest, UUID> {

    Optional<OtpRequest> findTopByPhoneAndPurposeOrderByCreatedAtDesc(String phone, OtpPurpose purpose);

    long countByPhoneAndCreatedAtAfter(String phone, Instant after);
}
