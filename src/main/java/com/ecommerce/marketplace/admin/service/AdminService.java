package com.ecommerce.marketplace.admin.service;

import com.ecommerce.marketplace.admin.dto.AdminLedgerEntryResponse;
import com.ecommerce.marketplace.admin.dto.AdminMetricsOverviewResponse;
import com.ecommerce.marketplace.admin.dto.AdminOrderResponse;
import com.ecommerce.marketplace.admin.dto.CustomerAccountResponse;
import com.ecommerce.marketplace.catalog.dto.ProductResponse;
import com.ecommerce.marketplace.catalog.model.Product;
import com.ecommerce.marketplace.catalog.model.ProductStatus;
import com.ecommerce.marketplace.catalog.repository.ProductRepository;
import com.ecommerce.marketplace.catalog.service.ProductService;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.finance.repository.LedgerEntryRepository;
import com.ecommerce.marketplace.identity.model.RoleName;
import com.ecommerce.marketplace.identity.model.UserRole;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.returns.model.ReturnStatus;
import com.ecommerce.marketplace.returns.repository.ReturnRequestRepository;
import com.ecommerce.marketplace.seller.dto.SellerProfileResponse;
import com.ecommerce.marketplace.seller.model.DocumentStatus;
import com.ecommerce.marketplace.seller.model.OnboardingStatus;
import com.ecommerce.marketplace.seller.model.Seller;
import com.ecommerce.marketplace.seller.model.SellerDocument;
import com.ecommerce.marketplace.seller.model.SellerStatus;
import com.ecommerce.marketplace.seller.repository.SellerDocumentRepository;
import com.ecommerce.marketplace.seller.repository.SellerRepository;
import com.ecommerce.marketplace.seller.service.SellerService;
import com.ecommerce.marketplace.support.model.TicketStatus;
import com.ecommerce.marketplace.support.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SellerRepository sellerRepository;
    private final SellerDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final SellerService sellerService;
    private final OrderRepository orderRepository;
    private final SupportTicketRepository ticketRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public SellerProfileResponse approveSeller(UUID adminId, UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId.toString()));

        seller.setStatus(SellerStatus.VERIFIED);
        seller.setOnboardingStatus(OnboardingStatus.APPROVED);
        seller = sellerRepository.save(seller);

        // Ensure user has ROLE_SELLER in identity_schema
        userRepository.findById(seller.getUserId()).ifPresent(user -> {
            boolean hasRole = user.getRoles().stream()
                .anyMatch(r -> r.getRole().equals(RoleName.ROLE_SELLER.name()));
            if (!hasRole) {
                UserRole role = UserRole.builder()
                    .user(user)
                    .role(RoleName.ROLE_SELLER.name())
                    .grantedBy(adminId)
                    .build();
                user.getRoles().add(role);
                userRepository.save(user);
            }
        });

        log.info("Admin {} approved seller {}", adminId, seller.getBusinessName());
        return sellerService.getSellerById(seller.getId());
    }

    @Transactional
    public SellerProfileResponse rejectSeller(UUID adminId, UUID sellerId, String reason) {
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller", sellerId.toString()));

        seller.setStatus(SellerStatus.REJECTED);
        seller = sellerRepository.save(seller);

        log.info("Admin {} rejected seller {}: {}", adminId, seller.getBusinessName(), reason);
        return sellerService.getSellerById(seller.getId());
    }

    @Transactional
    public void verifySellerDocument(UUID adminId, UUID documentId, boolean approved, String reason) {
        SellerDocument doc = documentRepository.findById(documentId)
            .orElseThrow(() -> new ResourceNotFoundException("Seller Document", documentId.toString()));

        doc.setStatus(approved ? DocumentStatus.APPROVED : DocumentStatus.REJECTED);
        doc.setVerifiedBy(adminId);
        doc.setVerifiedAt(Instant.now());
        if (!approved) {
            doc.setRejectionReason(reason);
        }
        documentRepository.save(doc);
        log.info("Admin {} verified document {}: approved={}", adminId, documentId, approved);
    }

    @Transactional
    public ProductResponse approveProduct(UUID adminId, UUID productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        product.setStatus(ProductStatus.APPROVED);
        product.setReviewedBy(adminId);
        product.setReviewedAt(Instant.now());
        product = productRepository.save(product);

        log.info("Admin {} approved product {}", adminId, product.getTitle());
        return productService.getProductById(product.getId());
    }

    @Transactional
    public ProductResponse rejectProduct(UUID adminId, UUID productId, String reason) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId.toString()));

        product.setStatus(ProductStatus.REJECTED);
        product.setRejectionReason(reason);
        product.setReviewedBy(adminId);
        product.setReviewedAt(Instant.now());
        product = productRepository.save(product);

        log.info("Admin {} rejected product {}: {}", adminId, product.getTitle(), reason);
        return productService.getProductById(product.getId());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getPendingProducts(Pageable pageable) {
        Page<Product> page = productRepository.findByStatus(ProductStatus.UNDER_REVIEW, pageable);
        List<ProductResponse> data = page.getContent().stream()
            .map(p -> productService.getProductById(p.getId()))
            .collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerProfileResponse> getSellers(SellerStatus status, Pageable pageable) {
        Page<Seller> page = status != null
            ? sellerRepository.findByStatus(status, pageable)
            : sellerRepository.findAll(pageable);

        List<SellerProfileResponse> data = page.getContent().stream()
            .map(s -> sellerService.getSellerById(s.getId()))
            .collect(Collectors.toList());
        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public AdminMetricsOverviewResponse getPlatformMetrics() {
        List<Order> allOrders = orderRepository.findAll();

        long gmvPaisa = allOrders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PAID ||
                         o.getStatus() == OrderStatus.PROCESSING ||
                         o.getStatus() == OrderStatus.SHIPPED ||
                         o.getStatus() == OrderStatus.DELIVERED)
            .mapToLong(Order::getTotalPaisa)
            .sum();

        Map<String, Long> ordersByStatus = allOrders.stream()
            .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));

        List<Seller> sellers = sellerRepository.findAll();
        long totalSellers = sellers.size();
        long verifiedSellers = sellers.stream().filter(s -> s.getStatus() == SellerStatus.VERIFIED).count();
        long pendingSellers = sellers.stream().filter(s -> s.getStatus() == SellerStatus.REGISTERED || s.getStatus() == SellerStatus.UNDER_REVIEW).count();

        List<Product> products = productRepository.findAll();
        long totalProducts = products.size();
        long approvedProducts = products.stream().filter(p -> p.getStatus() == ProductStatus.APPROVED).count();
        long pendingProducts = products.stream().filter(p -> p.getStatus() == ProductStatus.UNDER_REVIEW || p.getStatus() == ProductStatus.DRAFT).count();

        long totalCustomers = userRepository.count();

        long openTickets = ticketRepository.findByStatus(TicketStatus.OPEN, Pageable.unpaged()).getTotalElements();
        long escalatedTickets = ticketRepository.findAll().stream().filter(t -> Boolean.TRUE.equals(t.getIsEscalated())).count();

        long totalReturns = returnRequestRepository.count();

        return AdminMetricsOverviewResponse.builder()
            .gmvPaisa(gmvPaisa)
            .totalOrders((long) allOrders.size())
            .ordersByStatus(ordersByStatus)
            .totalSellers(totalSellers)
            .verifiedSellers(verifiedSellers)
            .pendingSellers(pendingSellers)
            .totalProducts(totalProducts)
            .approvedProducts(approvedProducts)
            .pendingProducts(pendingProducts)
            .totalCustomers(totalCustomers)
            .openTickets(openTickets)
            .escalatedTickets(escalatedTickets)
            .totalReturns(totalReturns)
            .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderResponse> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page = status != null
            ? orderRepository.findByStatus(status, pageable)
            : orderRepository.findAll(pageable);

        List<AdminOrderResponse> data = page.getContent().stream()
            .map(o -> {
                String customerName = "Customer";
                String customerEmail = "";
                var userOpt = userRepository.findById(o.getCustomerId());
                if (userOpt.isPresent()) {
                    var u = userOpt.get();
                    customerName = u.getName() != null ? u.getName() : "Customer " + u.getPhone();
                    customerEmail = u.getEmail() != null ? u.getEmail() : "";
                }
                String sla = "ON_TRACK";
                if (o.getStatus() == OrderStatus.PROCESSING || o.getStatus() == OrderStatus.PAID) {
                    if (o.getCreatedAt() != null && o.getCreatedAt().isBefore(Instant.now().minusSeconds(86400))) {
                        sla = "SLA_WARNING";
                    }
                }
                return AdminOrderResponse.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .customerId(o.getCustomerId())
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .amountPaisa(o.getTotalPaisa())
                    .status(o.getStatus())
                    .slaStatus(sla)
                    .itemCount(o.getItems() != null ? o.getItems().size() : 0)
                    .createdAt(o.getCreatedAt())
                    .build();
            })
            .collect(Collectors.toList());

        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerAccountResponse> getCustomers(Pageable pageable) {
        Page<com.ecommerce.marketplace.identity.model.User> page = userRepository.findAll(pageable);

        List<CustomerAccountResponse> data = page.getContent().stream()
            .map(u -> {
                List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(u.getId(), Pageable.unpaged()).getContent();
                long totalOrders = orders.size();
                long totalSpentPaisa = orders.stream()
                    .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                    .mapToLong(Order::getTotalPaisa)
                    .sum();

                return CustomerAccountResponse.builder()
                    .id(u.getId())
                    .name(u.getName() != null ? u.getName() : "Customer (" + u.getPhone() + ")")
                    .email(u.getEmail())
                    .phone(u.getPhone())
                    .status(u.getStatus())
                    .totalOrders(totalOrders)
                    .totalSpentPaisa(totalSpentPaisa)
                    .registeredAt(u.getCreatedAt())
                    .build();
            })
            .collect(Collectors.toList());

        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional
    public CustomerAccountResponse updateCustomerStatus(UUID customerId, String statusStr) {
        var user = userRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId.toString()));

        if (statusStr != null) {
            try {
                user.setStatus(com.ecommerce.marketplace.identity.model.UserStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                if ("ACTIVE".equalsIgnoreCase(statusStr)) {
                    user.setStatus(com.ecommerce.marketplace.identity.model.UserStatus.ACTIVE);
                } else {
                    user.setStatus(com.ecommerce.marketplace.identity.model.UserStatus.SUSPENDED);
                }
            }
        }
        user = userRepository.save(user);

        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged()).getContent();
        long totalOrders = orders.size();
        long totalSpentPaisa = orders.stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .mapToLong(Order::getTotalPaisa)
            .sum();

        return CustomerAccountResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .status(user.getStatus())
            .totalOrders(totalOrders)
            .totalSpentPaisa(totalSpentPaisa)
            .registeredAt(user.getCreatedAt())
            .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminLedgerEntryResponse> getFinanceLedger(Pageable pageable) {
        var page = ledgerEntryRepository.findAll(pageable);
        List<AdminLedgerEntryResponse> data = page.getContent().stream()
            .map(e -> AdminLedgerEntryResponse.builder()
                .id(e.getId())
                .transactionId(e.getTransactionId())
                .entryType(e.getEntryType() != null ? e.getEntryType().name() : "ENTRY")
                .amountPaisa(e.getAmountPaisa())
                .referenceType(e.getReferenceType())
                .referenceId(e.getReferenceId())
                .description(e.getDescription())
                .timestamp(e.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }
}
