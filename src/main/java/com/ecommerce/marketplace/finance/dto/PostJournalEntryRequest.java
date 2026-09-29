package com.ecommerce.marketplace.finance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostJournalEntryRequest {

    @NotNull(message = "Debit account ID is required")
    private UUID debitAccountId;

    @NotNull(message = "Credit account ID is required")
    private UUID creditAccountId;

    @NotNull(message = "Amount in paisa is required")
    @Min(value = 1, message = "Amount must be at least 1 paisa")
    private Long amountPaisa;

    private String referenceType;
    private UUID referenceId;
    private String description;
}
