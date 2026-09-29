package com.ecommerce.marketplace.returns.service;

import com.ecommerce.marketplace.catalog.repository.CategoryRepository;
import com.ecommerce.marketplace.catalog.repository.ProductRepository;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.inventory.service.InventoryService;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import com.ecommerce.marketplace.payment.repository.PaymentIntentRepository;
import com.ecommerce.marketplace.payment.service.PaymentService;
import com.ecommerce.marketplace.returns.dto.CreateReturnRequest;
import com.ecommerce.marketplace.returns.dto.InspectReturnRequest;
import com.ecommerce.marketplace.returns.dto.ReturnInspectionResponse;
import com.ecommerce.marketplace.returns.dto.ReturnResponse;
import com.ecommerce.marketplace.returns.model.*;
import com.ecommerce.marketplace.returns.repository.ReturnInspectionRepository;
import com.ecommerce.marketplace.returns.repository.ReturnRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private ReturnInspectionRepository inspectionRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReturnService returnService;

    private UUID customerId;
    private UUID orderId;
    private UUID orderItemId;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        orderItemId = UUID.randomUUID();

        orderItem = OrderItem.builder()
                .id(orderItemId)
                .variantId(UUID.randomUUID())
                .qty(1)
                .unitPricePaisa(150000L)
                .build();

        order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.DELIVERED)
                .items(new ArrayList<>(List.of(orderItem)))
                .build();
    }

    @Test
    @DisplayName("Should create return request for eligible delivered order")
    void testCreateReturnRequestSuccess() {
        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .returnType(ReturnType.REFUND)
                .reason(ReturnReason.DEFECTIVE)
                .customerNotes("Device not powering on")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(returnRequestRepository.findByOrderItemIdAndStatusNot(orderItemId, ReturnStatus.CANCELLED))
                .thenReturn(Optional.empty());
        when(productRepository.findAll()).thenReturn(List.of());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
            ReturnRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(customerId, request);

        assertNotNull(response);
        assertEquals(ReturnStatus.REQUESTED, response.getStatus());
        assertEquals(ReturnReason.DEFECTIVE, response.getReason());
        verify(returnRequestRepository).save(any(ReturnRequest.class));
    }

    @Test
    @DisplayName("Should reject return if order is not delivered")
    void testRejectIfNotDelivered() {
        order.setStatus(OrderStatus.SHIPPED);
        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .reason(ReturnReason.DEFECTIVE)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                returnService.createReturnRequest(customerId, request)
        );

        assertTrue(ex.getMessage().contains("Returns can only be requested for DELIVERED orders"));
    }

    @Test
    @DisplayName("Should reject return request if return window has expired")
    void testRejectIfWindowExpired() {
        order.setUpdatedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(orderId)
                .orderItemId(orderItemId)
                .reason(ReturnReason.NOT_AS_DESCRIBED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(returnRequestRepository.findByOrderItemIdAndStatusNot(orderItemId, ReturnStatus.CANCELLED))
                .thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                returnService.createReturnRequest(customerId, request)
        );

        assertTrue(ex.getMessage().contains("Return window"));
    }
}
