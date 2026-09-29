package com.ecommerce.marketplace.admin.service;

import com.ecommerce.marketplace.admin.dto.AdminMetricsOverviewResponse;
import com.ecommerce.marketplace.catalog.model.Product;
import com.ecommerce.marketplace.catalog.model.ProductStatus;
import com.ecommerce.marketplace.catalog.repository.ProductRepository;
import com.ecommerce.marketplace.catalog.service.ProductService;
import com.ecommerce.marketplace.identity.model.User;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.returns.repository.ReturnRequestRepository;
import com.ecommerce.marketplace.seller.dto.SellerProfileResponse;
import com.ecommerce.marketplace.seller.model.DocumentStatus;
import com.ecommerce.marketplace.seller.model.Seller;
import com.ecommerce.marketplace.seller.model.SellerDocument;
import com.ecommerce.marketplace.seller.model.SellerStatus;
import com.ecommerce.marketplace.seller.repository.SellerDocumentRepository;
import com.ecommerce.marketplace.seller.repository.SellerRepository;
import com.ecommerce.marketplace.seller.service.SellerService;
import com.ecommerce.marketplace.support.model.TicketStatus;
import com.ecommerce.marketplace.support.repository.SupportTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Mock
    private SellerService sellerService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private AdminService adminService;

    private UUID adminId;
    private UUID sellerId;
    private UUID userId;
    private Seller seller;
    private User user;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("seller@marketplace.com")
                .roles(new java.util.HashSet<>())
                .build();

        seller = Seller.builder()
                .id(sellerId)
                .userId(userId)
                .businessName("Acme Retailers")
                .status(SellerStatus.UNDER_REVIEW)
                .build();
    }

    @Test
    @DisplayName("Should approve seller and assign ROLE_SELLER")
    void testApproveSeller() {
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sellerService.getSellerById(sellerId)).thenReturn(SellerProfileResponse.builder()
                .id(sellerId)
                .businessName("Acme Retailers")
                .status(SellerStatus.VERIFIED)
                .build());

        SellerProfileResponse response = adminService.approveSeller(adminId, sellerId);

        assertNotNull(response);
        assertEquals(SellerStatus.VERIFIED, seller.getStatus());
        assertEquals(1, user.getRoles().size());
        assertEquals("ROLE_SELLER", user.getRoles().iterator().next().getRole());
        verify(sellerRepository).save(seller);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should verify seller document status")
    void testVerifySellerDocument() {
        UUID docId = UUID.randomUUID();
        SellerDocument doc = SellerDocument.builder()
                .id(docId)
                .sellerId(sellerId)
                .status(DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(SellerDocument.class))).thenAnswer(i -> i.getArgument(0));

        adminService.verifySellerDocument(adminId, docId, true, null);

        assertEquals(DocumentStatus.APPROVED, doc.getStatus());
        assertEquals(adminId, doc.getVerifiedBy());
        assertNotNull(doc.getVerifiedAt());
        verify(documentRepository).save(doc);
    }

    @Test
    @DisplayName("Should aggregate platform GMV and operational metrics correctly")
    void testPlatformMetrics() {
        List<Order> orders = List.of(
                Order.builder().status(OrderStatus.DELIVERED).totalPaisa(150000L).build(),
                Order.builder().status(OrderStatus.PAID).totalPaisa(250000L).build(),
                Order.builder().status(OrderStatus.CANCELLED).totalPaisa(50000L).build()
        );

        when(orderRepository.findAll()).thenReturn(orders);
        when(sellerRepository.findAll()).thenReturn(List.of(
                Seller.builder().status(SellerStatus.VERIFIED).build(),
                Seller.builder().status(SellerStatus.UNDER_REVIEW).build()
        ));
        when(productRepository.findAll()).thenReturn(List.of(
                Product.builder().status(ProductStatus.APPROVED).build(),
                Product.builder().status(ProductStatus.UNDER_REVIEW).build()
        ));
        when(userRepository.count()).thenReturn(100L);
        when(ticketRepository.findByStatus(eq(TicketStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(ticketRepository.findAll()).thenReturn(List.of());
        when(returnRequestRepository.count()).thenReturn(5L);

        AdminMetricsOverviewResponse metrics = adminService.getPlatformMetrics();

        assertNotNull(metrics);
        // Only DELIVERED and PAID count towards GMV: 150000 + 250000 = 400000 paisa
        assertEquals(400000L, metrics.getGmvPaisa());
        assertEquals(3L, metrics.getTotalOrders());
        assertEquals(2L, metrics.getTotalSellers());
        assertEquals(1L, metrics.getVerifiedSellers());
        assertEquals(1L, metrics.getPendingSellers());
        assertEquals(2L, metrics.getTotalProducts());
        assertEquals(1L, metrics.getApprovedProducts());
        assertEquals(100L, metrics.getTotalCustomers());
        assertEquals(5L, metrics.getTotalReturns());
    }
}
