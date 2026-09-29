package com.ecommerce.marketplace.review.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerReviewResponseRequest {

    @NotBlank(message = "Response message cannot be blank")
    private String response;
}
