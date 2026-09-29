package com.ecommerce.marketplace.seller.repository;

import com.ecommerce.marketplace.seller.model.DocumentType;
import com.ecommerce.marketplace.seller.model.SellerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerDocumentRepository extends JpaRepository<SellerDocument, UUID> {

    List<SellerDocument> findBySellerId(UUID sellerId);

    Optional<SellerDocument> findBySellerIdAndDocumentType(UUID sellerId, DocumentType documentType);
}
