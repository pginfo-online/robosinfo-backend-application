package com.ecommerce.marketplace.seller.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.identity.dto.AuthResponse;
import com.ecommerce.marketplace.identity.model.*;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import com.ecommerce.marketplace.identity.security.JwtTokenProvider;
import com.ecommerce.marketplace.identity.repository.RefreshTokenRepository;
import com.ecommerce.marketplace.seller.dto.*;
import com.ecommerce.marketplace.seller.model.*;
import com.ecommerce.marketplace.seller.repository.SellerBankAccountRepository;
import com.ecommerce.marketplace.seller.repository.SellerDocumentRepository;
import com.ecommerce.marketplace.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final SellerDocumentRepository documentRepository;
    private final SellerBankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public SellerProfileResponse registerSeller(UUID userId, RegisterSellerRequest request) {
        if (sellerRepository.findByUserId(userId).isPresent()) {
            throw new BusinessRuleException("User is already registered as a seller", "SELLER_ALREADY_EXISTS");
        }

        if (request.getGstNumber() != null && sellerRepository.existsByGstNumber(request.getGstNumber())) {
            throw new BusinessRuleException("GST number is already registered by another seller", "GST_ALREADY_EXISTS");
        }

        if (request.getPanNumber() != null && sellerRepository.existsByPanNumber(request.getPanNumber())) {
            throw new BusinessRuleException("PAN number is already registered by another seller", "PAN_ALREADY_EXISTS");
        }

        Seller seller = Seller.builder()
            .userId(userId)
            .businessName(request.getBusinessName())
            .businessType(request.getBusinessType())
            .gstNumber(request.getGstNumber())
            .panNumber(request.getPanNumber())
            .status(SellerStatus.REGISTERED)
            .onboardingStatus(OnboardingStatus.INCOMPLETE)
            .commissionRateBps(1000)
            .build();

        seller = sellerRepository.save(seller);

        // Grant ROLE_SELLER to user
        userRepository.findById(userId).ifPresent(user -> {
            user.addRole(RoleName.ROLE_SELLER);
            userRepository.save(user);
        });

        return toSellerProfileResponse(seller);
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerByUserId(UUID userId) {
        Seller seller = sellerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller profile", userId.toString()));
        return toSellerProfileResponse(seller);
    }

    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerById(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId.toString()));
        return toSellerProfileResponse(seller);
    }

    @Transactional
    public DocumentResponse uploadDocument(UUID sellerId, DocumentUploadRequest request) {
        // Verify seller exists
        if (!sellerRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("Seller", sellerId.toString());
        }

        // Upsert document of this type
        SellerDocument doc = documentRepository.findBySellerIdAndDocumentType(sellerId, request.getDocumentType())
            .orElseGet(() -> SellerDocument.builder()
                .sellerId(sellerId)
                .documentType(request.getDocumentType())
                .build());

        doc.setFileUrl(request.getFileUrl());
        doc.setStatus(DocumentStatus.PENDING);
        doc.setRejectionReason(null);

        doc = documentRepository.save(doc);
        return toDocumentResponse(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(UUID sellerId) {
        return documentRepository.findBySellerId(sellerId).stream()
            .map(this::toDocumentResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public BankAccountResponse addBankAccount(UUID sellerId, BankAccountRequest request) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("Seller", sellerId.toString());
        }

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            bankAccountRepository.resetPrimaryBankAccounts(sellerId);
        }

        SellerBankAccount bankAccount = SellerBankAccount.builder()
            .sellerId(sellerId)
            .accountHolder(request.getAccountHolder())
            .accountNumberEncrypted(request.getAccountNumber()) // In prod: encrypt with AES-GCM
            .ifscCode(request.getIfscCode())
            .bankName(request.getBankName())
            .isVerified(false)
            .isPrimary(Boolean.TRUE.equals(request.getIsPrimary()))
            .build();

        bankAccount = bankAccountRepository.save(bankAccount);
        return toBankAccountResponse(bankAccount);
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getBankAccounts(UUID sellerId) {
        return bankAccountRepository.findBySellerId(sellerId).stream()
            .map(this::toBankAccountResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public SellerProfileResponse submitForReview(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId.toString()));

        List<SellerDocument> documents = documentRepository.findBySellerId(sellerId);
        if (documents.isEmpty()) {
            throw new BusinessRuleException("Please upload KYC documents before submitting for review", "DOCUMENTS_REQUIRED");
        }

        List<SellerBankAccount> bankAccounts = bankAccountRepository.findBySellerId(sellerId);
        if (bankAccounts.isEmpty()) {
            throw new BusinessRuleException("Please add a bank account before submitting for review", "BANK_ACCOUNT_REQUIRED");
        }

        seller.setOnboardingStatus(OnboardingStatus.SUBMITTED);
        seller.setStatus(SellerStatus.UNDER_REVIEW);
        seller = sellerRepository.save(seller);

        return toSellerProfileResponse(seller);
    }

    // ─── Seller Business Registration (combined User + Seller creation) ───
    @Transactional
    public SellerAuthResponse registerBusiness(RegisterBusinessRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email is already registered", "EMAIL_ALREADY_EXISTS");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessRuleException("Phone number is already registered", "PHONE_ALREADY_EXISTS");
        }

        // Create user
        User user = User.builder()
            .phone(request.getPhone())
            .email(request.getEmail())
            .name(request.getCompanyName())
            .userType(UserType.SELLER)
            .status(UserStatus.ACTIVE)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .build();
        user.addRole(RoleName.ROLE_SELLER);
        user = userRepository.save(user);

        // Create seller profile
        Seller seller = Seller.builder()
            .userId(user.getId())
            .businessName(request.getCompanyName())
            .businessType(request.getBusinessType() != null ? request.getBusinessType() : BusinessType.PROPRIETORSHIP)
            .gstNumber(request.getGstin())
            .status(SellerStatus.REGISTERED)
            .onboardingStatus(OnboardingStatus.INCOMPLETE)
            .commissionRateBps(1000)
            .supportEmail(request.getEmail())
            .supportPhone(request.getPhone())
            .build();
        seller = sellerRepository.save(seller);

        // Generate tokens
        Set<String> roles = user.getRoles().stream().map(UserRole::getRole).collect(Collectors.toSet());
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getPhone(), roles);
        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        RefreshToken refreshTokenEntity = RefreshToken.builder()
            .userId(user.getId())
            .tokenHash(hashString(rawRefreshToken))
            .tokenFamily(UUID.randomUUID())
            .expiresAt(Instant.now().plus(Duration.ofMillis(tokenProvider.getRefreshTokenExpiryMs())))
            .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return SellerAuthResponse.builder()
            .seller(toSellerProfileResponse(seller))
            .accessToken(accessToken)
            .refreshToken(rawRefreshToken)
            .expiresIn(tokenProvider.getAccessTokenExpiryMs())
            .build();
    }

    // ─── Store Settings ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public StoreSettingsResponse getStoreSettings(UUID userId) {
        Seller seller = sellerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller profile", userId.toString()));
        List<SellerBankAccount> bankAccounts = bankAccountRepository.findBySellerId(seller.getId());
        boolean hasBankAccount = !bankAccounts.isEmpty();
        String maskedBank = "";
        if (hasBankAccount) {
            SellerBankAccount primary = bankAccounts.stream()
                .filter(SellerBankAccount::getIsPrimary).findFirst().orElse(bankAccounts.get(0));
            String raw = primary.getAccountNumberEncrypted();
            maskedBank = raw.length() > 4 ? "••••" + raw.substring(raw.length() - 4) : raw;
        }
        List<SellerDocument> docs = documentRepository.findBySellerId(seller.getId());
        boolean kycVerified = docs.stream().anyMatch(d -> d.getStatus() == DocumentStatus.APPROVED);

        return StoreSettingsResponse.builder()
            .sellerId(seller.getId())
            .businessName(seller.getBusinessName())
            .supportEmail(seller.getSupportEmail())
            .supportPhone(seller.getSupportPhone())
            .storeDescription(seller.getStoreDescription())
            .pickupAddress(seller.getPickupAddress())
            .returnPolicyDays(seller.getReturnPolicyDays())
            .gstNumber(seller.getGstNumber())
            .panNumber(seller.getPanNumber())
            .kycVerified(kycVerified)
            .bankAccountConfigured(hasBankAccount)
            .maskedBankAccount(maskedBank)
            .build();
    }

    @Transactional
    public StoreSettingsResponse updateStoreSettings(UUID userId, StoreSettingsRequest request) {
        Seller seller = sellerRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller profile", userId.toString()));

        if (request.getBusinessName() != null) seller.setBusinessName(request.getBusinessName());
        if (request.getSupportEmail() != null) seller.setSupportEmail(request.getSupportEmail());
        if (request.getSupportPhone() != null) seller.setSupportPhone(request.getSupportPhone());
        if (request.getStoreDescription() != null) seller.setStoreDescription(request.getStoreDescription());
        if (request.getPickupAddress() != null) seller.setPickupAddress(request.getPickupAddress());
        if (request.getReturnPolicyDays() != null) seller.setReturnPolicyDays(request.getReturnPolicyDays());

        seller = sellerRepository.save(seller);
        return getStoreSettings(userId);
    }

    // ─── Mapping helpers ────────────────────────────────────────────────
    public SellerProfileResponse toSellerProfileResponse(Seller seller) {
        User user = userRepository.findById(seller.getUserId()).orElse(null);
        return SellerProfileResponse.builder()
            .id(seller.getId())
            .userId(seller.getUserId())
            .businessName(seller.getBusinessName())
            .businessType(seller.getBusinessType())
            .gstNumber(seller.getGstNumber())
            .panNumber(seller.getPanNumber())
            .status(seller.getStatus())
            .onboardingStatus(seller.getOnboardingStatus())
            .commissionRateBps(seller.getCommissionRateBps())
            .rating(seller.getRating())
            .totalOrders(seller.getTotalOrders())
            .email(user != null ? user.getEmail() : null)
            .phone(user != null ? user.getPhone() : null)
            .build();
    }

    private DocumentResponse toDocumentResponse(SellerDocument doc) {
        return DocumentResponse.builder()
            .id(doc.getId())
            .documentType(doc.getDocumentType())
            .fileUrl(doc.getFileUrl())
            .status(doc.getStatus())
            .rejectionReason(doc.getRejectionReason())
            .uploadedAt(doc.getUploadedAt())
            .verifiedAt(doc.getVerifiedAt())
            .build();
    }

    private BankAccountResponse toBankAccountResponse(SellerBankAccount bank) {
        String raw = bank.getAccountNumberEncrypted();
        String masked = raw.length() > 4 ? "X".repeat(raw.length() - 4) + raw.substring(raw.length() - 4) : raw;

        return BankAccountResponse.builder()
            .id(bank.getId())
            .accountHolder(bank.getAccountHolder())
            .maskedAccountNumber(masked)
            .ifscCode(bank.getIfscCode())
            .bankName(bank.getBankName())
            .isVerified(bank.getIsVerified())
            .isPrimary(bank.getIsPrimary())
            .createdAt(bank.getCreatedAt())
            .build();
    }

    private String hashString(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
