package com.ecommerce.marketplace.search.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacetCount {
    private UUID id;
    private String name;
    private Long count;
}
