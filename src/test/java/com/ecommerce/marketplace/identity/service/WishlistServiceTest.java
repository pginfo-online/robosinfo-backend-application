package com.ecommerce.marketplace.identity.service;

import com.ecommerce.marketplace.catalog.model.*;
import com.ecommerce.marketplace.catalog.repository.*;
import com.ecommerce.marketplace.identity.dto.WishlistItemResponse;
import com.ecommerce.marketplace.identity.dto.WishlistResponse;
import com.ecommerce.marketplace.identity.model.WishlistItem;
import com.ecommerce.marketplace.identity.repository.WishlistRepository;
import com.ecommerce.marketplace.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SellerListingRepository listingRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductImageRepository imageRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private UUID customerId;
    private UUID productId;
    private Product product;
    private Category category;
    private Brand brand;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();

        category = Category.builder().id(categoryId).name("Smartphones").build();
        brand = Brand.builder().id(brandId).name("Apex").build();

        ProductVariant variant = ProductVariant.builder()
            .id(UUID.randomUUID())
            .sku("APEX-1-BLK")
            .isActive(true)
            .build();

        product = Product.builder()
            .id(productId)
            .title("Apex Phone 1")
            .slug("apex-phone-1")
            .categoryId(categoryId)
            .brandId(brandId)
            .status(ProductStatus.APPROVED)
            .variants(List.of(variant))
            .build();
    }

    @Test
    @DisplayName("Should add product to wishlist successfully")
    void shouldAddProductToWishlist() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wishlistRepository.findByCustomerIdAndProductId(customerId, productId)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any(WishlistItem.class))).thenAnswer(i -> {
            WishlistItem item = i.getArgument(0);
            item.setId(UUID.randomUUID());
            item.setCreatedAt(Instant.now());
            return item;
        });
        when(categoryRepository.findById(any())).thenReturn(Optional.of(category));
        when(brandRepository.findById(any())).thenReturn(Optional.of(brand));
        when(listingRepository.findFirstByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(any())).thenReturn(Optional.empty());
        when(imageRepository.findByProductIdOrderByDisplayOrderAsc(any())).thenReturn(Collections.emptyList());
        when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(4.5);
        when(reviewRepository.countByProductId(any())).thenReturn(10L);

        WishlistItemResponse response = wishlistService.addToWishlist(customerId, productId);

        assertNotNull(response);
        assertEquals(productId, response.getProductId());
        assertEquals("Apex Phone 1", response.getTitle());
        assertEquals("Smartphones", response.getCategoryName());
        assertEquals(4.5, response.getAverageRating());
        verify(wishlistRepository).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("Should remove product from wishlist successfully")
    void shouldRemoveFromWishlist() {
        wishlistService.removeFromWishlist(customerId, productId);
        verify(wishlistRepository).deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Test
    @DisplayName("Should retrieve full wishlist for customer")
    void shouldGetWishlist() {
        WishlistItem item = WishlistItem.builder()
            .id(UUID.randomUUID())
            .customerId(customerId)
            .productId(productId)
            .createdAt(Instant.now())
            .build();

        when(wishlistRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(item));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(any())).thenReturn(Optional.of(category));
        when(brandRepository.findById(any())).thenReturn(Optional.of(brand));
        when(listingRepository.findFirstByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(any())).thenReturn(Optional.empty());
        when(imageRepository.findByProductIdOrderByDisplayOrderAsc(any())).thenReturn(Collections.emptyList());
        when(reviewRepository.getAverageRatingByProductId(any())).thenReturn(4.0);
        when(reviewRepository.countByProductId(any())).thenReturn(5L);

        WishlistResponse response = wishlistService.getWishlist(customerId);

        assertNotNull(response);
        assertEquals(1, response.getTotalItems());
        assertEquals("Apex Phone 1", response.getItems().get(0).getTitle());
    }
}
