package com.ecommerce.marketplace.delivery.dto;

import com.ecommerce.marketplace.delivery.model.ShipmentStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentResponse {
    private UUID id;
    private UUID orderId;
    private UUID sellerId;
    private UUID warehouseId;
    private String awbNumber;
    private ShipmentStatus status;
    private String carrierName;
    private String trackingUrl;
    private UUID deliveryPartnerId;
    private String deliveryOtp; // only populated for customer view or internal testing
    private Integer otpAttempts;
    private Instant estimatedDeliveryAt;
    private Instant deliveredAt;
    private String failedReason;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddressSnapshot;
    private Long codAmountPaisa;
    private Boolean isCod;
    private List<ShipmentItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}
