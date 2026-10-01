package com.ecommerce.marketplace.admin.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLedgerEntryResponse {
    private UUID id;
    private UUID transactionId;
    private String entryType;
    private Long amountPaisa;
    private String referenceType;
    private UUID referenceId;
    private String description;
    private Instant timestamp;
}
