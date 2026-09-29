package com.ecommerce.marketplace.delivery.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.delivery.dto.ShipmentResponse;
import com.ecommerce.marketplace.delivery.model.Shipment;
import com.ecommerce.marketplace.delivery.model.ShipmentStatus;
import com.ecommerce.marketplace.delivery.repository.ShipmentItemRepository;
import com.ecommerce.marketplace.delivery.repository.ShipmentRepository;
import com.ecommerce.marketplace.notification.service.NotificationHub;
import com.ecommerce.marketplace.order.model.Order;
import com.ecommerce.marketplace.order.model.OrderItem;
import com.ecommerce.marketplace.order.model.OrderStatus;
import com.ecommerce.marketplace.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentItemRepository shipmentItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationHub notificationHub;

    @InjectMocks
    private ShipmentService shipmentService;

    private UUID shipmentId;
    private UUID partnerId;
    private UUID orderId;
    private Shipment shipment;
    private Order order;

    @BeforeEach
    void setUp() {
        shipmentId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-2026-TEST")
                .customerId(UUID.randomUUID())
                .status(OrderStatus.PROCESSING)
                .items(new ArrayList<>())
                .build();

        shipment = Shipment.builder()
                .id(shipmentId)
                .orderId(orderId)
                .awbNumber("AWB-TEST-1234")
                .status(ShipmentStatus.ASSIGNED)
                .deliveryPartnerId(partnerId)
                .deliveryOtp("4321")
                .otpAttempts(0)
                .maxOtpAttempts(3)
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should mark shipment out for delivery and generate 4-digit OTP")
    void testMarkOutForDelivery() {
        shipment.setStatus(ShipmentStatus.PICKED_UP);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ShipmentResponse response = shipmentService.markOutForDelivery(partnerId, shipmentId);

        assertNotNull(response);
        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY, response.getStatus());
        assertNotNull(shipment.getDeliveryOtp());
        assertEquals(4, shipment.getDeliveryOtp().length());
        verify(notificationHub).dispatchOutForDelivery(eq(order.getCustomerId()), any(), eq(shipment.getAwbNumber()), any());
    }

    @Test
    @DisplayName("Should verify valid delivery OTP and mark shipment DELIVERED")
    void testVerifyDeliveryOtpSuccess() {
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setDeliveryOtp("7890");
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(List.of(shipment));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        ShipmentResponse response = shipmentService.verifyDeliveryOtp(partnerId, shipmentId, "7890");

        assertNotNull(response);
        assertEquals(ShipmentStatus.DELIVERED, response.getStatus());
        assertNotNull(shipment.getDeliveredAt());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Should increment OTP attempts and reject on invalid OTP")
    void testVerifyDeliveryOtpInvalid() {
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setDeliveryOtp("7890");
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(i -> i.getArgument(0));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                shipmentService.verifyDeliveryOtp(partnerId, shipmentId, "0000")
        );

        assertTrue(ex.getMessage().contains("Invalid delivery OTP"));
        assertEquals(1, shipment.getOtpAttempts());
    }
}
