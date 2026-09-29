package com.ecommerce.marketplace.cart.service;

import com.ecommerce.marketplace.cart.dto.*;
import com.ecommerce.marketplace.cart.model.Cart;
import com.ecommerce.marketplace.cart.model.CartItem;
import com.ecommerce.marketplace.cart.model.CartStatus;
import com.ecommerce.marketplace.cart.repository.CartItemRepository;
import com.ecommerce.marketplace.cart.repository.CartRepository;
import com.ecommerce.marketplace.catalog.model.Product;
import com.ecommerce.marketplace.catalog.model.ProductVariant;
import com.ecommerce.marketplace.catalog.model.SellerListing;
import com.ecommerce.marketplace.catalog.repository.ProductImageRepository;
import com.ecommerce.marketplace.catalog.repository.ProductRepository;
import com.ecommerce.marketplace.catalog.repository.ProductVariantRepository;
import com.ecommerce.marketplace.catalog.repository.SellerListingRepository;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.inventory.dto.InventoryStockResponse;
import com.ecommerce.marketplace.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final SellerListingRepository listingRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryService inventoryService;

    @Transactional
    public Cart getOrCreateCart(UUID customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
            .orElseGet(() -> cartRepository.save(Cart.builder()
                .customerId(customerId)
                .status(CartStatus.ACTIVE)
                .build()));
    }

    @Transactional
    public CartResponse addItem(UUID customerId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(customerId);

        UUID listingId = request.getSellerListingId();
        SellerListing listing;
        if (listingId != null) {
            listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller Listing", listingId.toString()));
        } else {
            listing = listingRepository.findFirstByVariantIdAndIsActiveTrueOrderBySellingPricePaisaAsc(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Active seller listing for variant", request.getVariantId().toString()));
        }

        CartItem item = cartItemRepository.findByCartIdAndSellerListingId(cart.getId(), listing.getId())
            .orElseGet(() -> CartItem.builder()
                .cart(cart)
                .variantId(request.getVariantId())
                .sellerListingId(listing.getId())
                .qty(0)
                .build());

        item.setQty(item.getQty() + request.getQty());
        cartItemRepository.save(item);

        return getCartDetails(customerId);
    }

    @Transactional
    public CartResponse updateItem(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId.toString()));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new ResourceNotFoundException("Cart item", itemId.toString());
        }

        if (request.getQty() <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQty(request.getQty());
            cartItemRepository.save(item);
        }

        return getCartDetails(customerId);
    }

    @Transactional
    public CartResponse removeItem(UUID customerId, UUID itemId) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId.toString()));

        if (item.getCart().getId().equals(cart.getId())) {
            cartItemRepository.delete(item);
        }

        return getCartDetails(customerId);
    }

    @Transactional
    public void clearCart(UUID customerId) {
        cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
            .ifPresent(cart -> cartItemRepository.deleteByCartId(cart.getId()));
    }

    @Transactional(readOnly = true)
    public CartResponse getCartDetails(UUID customerId) {
        Cart cart = cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
            .orElse(null);

        if (cart == null) {
            return CartResponse.builder()
                .customerId(customerId)
                .items(List.of())
                .subtotalPaisa(0L)
                .totalSavingsPaisa(0L)
                .itemCount(0)
                .build();
        }

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = new ArrayList<>();
        long subtotal = 0L;
        long totalSavings = 0L;
        int count = 0;

        for (CartItem item : items) {
            SellerListing listing = listingRepository.findById(item.getSellerListingId()).orElse(null);
            if (listing == null || !Boolean.TRUE.equals(listing.getIsActive())) {
                continue;
            }

            ProductVariant variant = variantRepository.findById(item.getVariantId()).orElse(null);
            Product product = variant != null ? productRepository.findById(variant.getProduct().getId()).orElse(null) : null;

            InventoryStockResponse stock = inventoryService.getStock(item.getVariantId(), listing.getSellerId());
            boolean inStock = stock.getAvailableQty() >= item.getQty();

            long itemSubtotal = listing.getSellingPricePaisa() * item.getQty();
            long itemSavings = Math.max(0, (listing.getMrpPaisa() - listing.getSellingPricePaisa()) * item.getQty());

            subtotal += itemSubtotal;
            totalSavings += itemSavings;
            count += item.getQty();

            String imageUrl = null;
            if (product != null) {
                var images = imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
                if (!images.isEmpty()) {
                    imageUrl = images.get(0).getUrl();
                }
            }

            itemResponses.add(CartItemResponse.builder()
                .id(item.getId())
                .variantId(item.getVariantId())
                .sellerListingId(item.getSellerListingId())
                .productTitle(product != null ? product.getTitle() : "Unknown")
                .productSlug(product != null ? product.getSlug() : null)
                .sku(variant != null ? variant.getSku() : "Unknown")
                .variantAttributes(variant != null ? variant.getVariantAttributes() : null)
                .imageUrl(imageUrl)
                .unitPricePaisa(listing.getSellingPricePaisa())
                .mrpPaisa(listing.getMrpPaisa())
                .qty(item.getQty())
                .subtotalPaisa(itemSubtotal)
                .inStock(inStock)
                .availableQty(stock.getAvailableQty())
                .build());
        }

        return CartResponse.builder()
            .cartId(cart.getId())
            .customerId(customerId)
            .items(itemResponses)
            .subtotalPaisa(subtotal)
            .totalSavingsPaisa(totalSavings)
            .itemCount(count)
            .build();
    }
}
