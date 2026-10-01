package com.ecommerce.marketplace.admin.dto;

import com.ecommerce.marketplace.order.model.OrderStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderResponse {
    private UUID id;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private Long amountPaisa;
    private OrderStatus status;
    private String slaStatus;
    private Integer itemCount;
    private Instant createdAt;
}
