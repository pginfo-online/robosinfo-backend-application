package com.ecommerce.marketplace.catalog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product_images", schema = "catalog_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
}
