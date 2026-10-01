package com.ecommerce.marketplace.inventory.service;

import com.ecommerce.marketplace.catalog.model.Product;
import com.ecommerce.marketplace.catalog.model.ProductImage;
import com.ecommerce.marketplace.catalog.model.ProductVariant;
import com.ecommerce.marketplace.catalog.repository.ProductImageRepository;
import com.ecommerce.marketplace.catalog.repository.ProductVariantRepository;
import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.inventory.dto.*;
import com.ecommerce.marketplace.inventory.model.*;
import com.ecommerce.marketplace.inventory.repository.InventoryLedgerRepository;
import com.ecommerce.marketplace.inventory.repository.InventoryReservationRepository;
import com.ecommerce.marketplace.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryLedgerRepository ledgerRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;

    @Value("${app.inventory.reservation-expiry-minutes:10}")
    private int reservationExpiryMinutes;

    private static final UUID DEFAULT_WAREHOUSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");


    @Transactional
    public InventoryStockResponse adjustStock(UUID sellerId, AdjustStockRequest request) {
        UUID warehouseId = request.getWarehouseId() != null ? request.getWarehouseId() : DEFAULT_WAREHOUSE_ID;

        Inventory inventory = inventoryRepository.findByVariantIdAndSellerIdForUpdate(request.getVariantId(), sellerId)
            .orElseGet(() -> Inventory.builder()
                .variantId(request.getVariantId())
                .sellerId(sellerId)
                .warehouseId(warehouseId)
                .physicalQty(0)
                .reservedQty(0)
                .damagedQty(0)
                .build());

        int newPhysical = inventory.getPhysicalQty() + request.getQtyChange();
        if (newPhysical < inventory.getReservedQty() + inventory.getDamagedQty()) {
            throw new BusinessRuleException("Resulting physical inventory cannot be less than reserved + damaged stock", "STOCK_UNDERFLOW");
        }

        inventory.setPhysicalQty(newPhysical);
        inventory = inventoryRepository.save(inventory);

        // Record immutable ledger entry
        InventoryLedger ledger = InventoryLedger.builder()
            .inventoryId(inventory.getId())
            .eventType(request.getEventType() != null ? request.getEventType() : InventoryEventType.ADJUSTMENT)
            .qtyChange(request.getQtyChange())
            .referenceType("MANUAL_ADJUSTMENT")
            .reason(request.getReason())
            .actorId(sellerId)
            .build();
        ledgerRepository.save(ledger);

        return toStockResponse(inventory);
    }

    @Transactional
    public ReservationResponse reserveStock(ReserveStockRequest request) {
        UUID key = request.getReservationKey() != null ? request.getReservationKey() : UUID.randomUUID();

        // Check if reservation already exists (idempotency)
        var existing = reservationRepository.findByReservationKey(key);
        if (existing.isPresent()) {
            return toReservationResponse(existing.get());
        }

        // Lock inventory row exclusively to prevent race conditions
        Inventory inventory = inventoryRepository.findByVariantIdAndSellerIdForUpdate(request.getVariantId(), request.getSellerId())
            .orElseThrow(() -> new BusinessRuleException("No inventory found for specified variant and seller", "INVENTORY_NOT_FOUND"));

        if (inventory.getAvailableQty() < request.getQty()) {
            throw new BusinessRuleException(
                "Insufficient stock: requested " + request.getQty() + ", available " + inventory.getAvailableQty(),
                "INSUFFICIENT_STOCK"
            );
        }

        // Hold inventory
        inventory.setReservedQty(inventory.getReservedQty() + request.getQty());
        inventoryRepository.save(inventory);

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(reservationExpiryMinutes));
        InventoryReservation reservation = InventoryReservation.builder()
            .inventoryId(inventory.getId())
            .orderId(request.getOrderId())
            .cartId(request.getCartId())
            .reservationKey(key)
            .qty(request.getQty())
            .status(ReservationStatus.HELD)
            .expiresAt(expiresAt)
            .build();

        reservation = reservationRepository.save(reservation);

        // Record reservation in ledger
        InventoryLedger ledger = InventoryLedger.builder()
            .inventoryId(inventory.getId())
            .eventType(InventoryEventType.RESERVED)
            .qtyChange(request.getQty())
            .referenceType("RESERVATION")
            .referenceId(reservation.getId())
            .reason("Held for order/checkout")
            .build();
        ledgerRepository.save(ledger);

        return toReservationResponse(reservation);
    }

    @Transactional
    public void confirmReservation(UUID reservationKey) {
        InventoryReservation reservation = reservationRepository.findByReservationKey(reservationKey)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationKey.toString()));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return; // Idempotent
        }

        if (reservation.getStatus() != ReservationStatus.HELD) {
            throw new BusinessRuleException("Cannot confirm reservation in status: " + reservation.getStatus(), "INVALID_RESERVATION_STATUS");
        }

        Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory", reservation.getInventoryId().toString()));

        // Convert reservation to actual stock decrement
        inventory.setReservedQty(Math.max(0, inventory.getReservedQty() - reservation.getQty()));
        inventory.setPhysicalQty(Math.max(0, inventory.getPhysicalQty() - reservation.getQty()));
        inventoryRepository.save(inventory);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        InventoryLedger ledger = InventoryLedger.builder()
            .inventoryId(inventory.getId())
            .eventType(InventoryEventType.CONFIRMED)
            .qtyChange(-reservation.getQty())
            .referenceType("ORDER_CONFIRMED")
            .referenceId(reservation.getOrderId())
            .reason("Deducted physical stock upon successful order placement")
            .build();
        ledgerRepository.save(ledger);
    }

    @Transactional
    public void releaseReservation(UUID reservationKey) {
        InventoryReservation reservation = reservationRepository.findByReservationKey(reservationKey)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationKey.toString()));

        if (reservation.getStatus() == ReservationStatus.RELEASED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            return; // Idempotent
        }

        if (reservation.getStatus() != ReservationStatus.HELD) {
            return;
        }

        Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory", reservation.getInventoryId().toString()));

        inventory.setReservedQty(Math.max(0, inventory.getReservedQty() - reservation.getQty()));
        inventoryRepository.save(inventory);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(Instant.now());
        reservationRepository.save(reservation);

        InventoryLedger ledger = InventoryLedger.builder()
            .inventoryId(inventory.getId())
            .eventType(InventoryEventType.RELEASED)
            .qtyChange(-reservation.getQty())
            .referenceType("RESERVATION_RELEASE")
            .referenceId(reservation.getId())
            .reason("Released reservation back to available inventory")
            .build();
        ledgerRepository.save(ledger);
    }

    @Transactional
    public void expireReservation(UUID reservationKey) {
        InventoryReservation reservation = reservationRepository.findByReservationKey(reservationKey)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationKey.toString()));

        if (reservation.getStatus() == ReservationStatus.RELEASED || reservation.getStatus() == ReservationStatus.EXPIRED) {
            return; // Idempotent
        }

        if (reservation.getStatus() != ReservationStatus.HELD) {
            return;
        }

        Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Inventory", reservation.getInventoryId().toString()));

        inventory.setReservedQty(Math.max(0, inventory.getReservedQty() - reservation.getQty()));
        inventoryRepository.save(inventory);

        reservation.setStatus(ReservationStatus.EXPIRED);
        reservation.setReleasedAt(Instant.now());
        reservationRepository.save(reservation);

        InventoryLedger ledger = InventoryLedger.builder()
            .inventoryId(inventory.getId())
            .eventType(InventoryEventType.RELEASED)
            .qtyChange(-reservation.getQty())
            .referenceType("RESERVATION_EXPIRED")
            .referenceId(reservation.getId())
            .reason("Expired reservation automatically released back to available inventory")
            .build();
        ledgerRepository.save(ledger);
    }

    @Transactional(readOnly = true)
    public InventoryStockResponse getStock(UUID variantId, UUID sellerId) {
        return inventoryRepository.findByVariantIdAndSellerId(variantId, sellerId)
            .map(this::toStockResponse)
            .orElse(InventoryStockResponse.builder()
                .variantId(variantId)
                .sellerId(sellerId)
                .physicalQty(0)
                .reservedQty(0)
                .damagedQty(0)
                .availableQty(0)
                .build());
    }

    @Transactional(readOnly = true)
    public List<InventoryStockResponse> getStockBySeller(UUID sellerId) {
        return inventoryRepository.findBySellerId(sellerId).stream()
            .map(this::toStockResponse)
            .collect(Collectors.toList());
    }

    public InventoryStockResponse toStockResponse(Inventory inv) {
        return InventoryStockResponse.builder()
            .id(inv.getId())
            .variantId(inv.getVariantId())
            .sellerId(inv.getSellerId())
            .warehouseId(inv.getWarehouseId())
            .physicalQty(inv.getPhysicalQty())
            .reservedQty(inv.getReservedQty())
            .damagedQty(inv.getDamagedQty())
            .availableQty(inv.getAvailableQty())
            .build();
    }

    private ReservationResponse toReservationResponse(InventoryReservation res) {
        return ReservationResponse.builder()
            .reservationId(res.getId())
            .reservationKey(res.getReservationKey())
            .inventoryId(res.getInventoryId())
            .qty(res.getQty())
            .status(res.getStatus())
            .expiresAt(res.getExpiresAt())
            .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryDetailResponse> getMyStockEnriched(
            UUID sellerId, Pageable pageable, String statusFilter, String search) {

        Page<Inventory> page = inventoryRepository.findBySellerId(sellerId, pageable);

        List<InventoryDetailResponse> enrichedList = page.getContent().stream()
            .map(this::toInventoryDetailResponse)
            .filter(item -> {
                if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
                    if (!statusFilter.equalsIgnoreCase(item.getStockStatus())) {
                        return false;
                    }
                }
                if (search != null && !search.isBlank()) {
                    String q = search.toLowerCase().trim();
                    return (item.getProductTitle() != null && item.getProductTitle().toLowerCase().contains(q))
                        || (item.getSku() != null && item.getSku().toLowerCase().contains(q));
                }
                return true;
            })
            .collect(Collectors.toList());

        return new PageResponse<>(enrichedList, page.getTotalElements(), null, page.hasNext());
    }

    @Transactional
    public void updateSafetyStock(UUID sellerId, UUID variantId, int safetyStock) {
        Inventory inv = inventoryRepository.findByVariantIdAndSellerId(variantId, sellerId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory", variantId.toString()));
        inv.setSafetyBuffer(safetyStock);
        inventoryRepository.save(inv);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockAuditLogResponse> getInventoryAuditLogs(UUID sellerId, Pageable pageable) {
        List<Inventory> sellerInvs = inventoryRepository.findBySellerId(sellerId);
        if (sellerInvs.isEmpty()) {
            return PageResponse.empty();
        }

        List<UUID> invIds = sellerInvs.stream().map(Inventory::getId).collect(Collectors.toList());
        Map<UUID, Inventory> invMap = sellerInvs.stream().collect(Collectors.toMap(Inventory::getId, i -> i));

        Page<InventoryLedger> ledgerPage = ledgerRepository.findByInventoryIdInOrderByCreatedAtDesc(invIds, pageable);

        List<StockAuditLogResponse> logs = ledgerPage.getContent().stream()
            .map(ledger -> {
                Inventory inv = invMap.get(ledger.getInventoryId());
                UUID variantId = inv != null ? inv.getVariantId() : null;
                ProductVariant variant = variantId != null ? variantRepository.findById(variantId).orElse(null) : null;
                Product product = variant != null ? variant.getProduct() : null;

                return StockAuditLogResponse.builder()
                    .id(ledger.getId())
                    .inventoryId(ledger.getInventoryId())
                    .variantId(variantId)
                    .sku(variant != null ? variant.getSku() : "N/A")
                    .productTitle(product != null ? product.getTitle() : "Unknown Product")
                    .eventType(ledger.getEventType())
                    .qtyChange(ledger.getQtyChange())
                    .referenceType(ledger.getReferenceType())
                    .referenceId(ledger.getReferenceId())
                    .reason(ledger.getReason())
                    .actorId(ledger.getActorId())
                    .createdAt(ledger.getCreatedAt())
                    .build();
            })
            .collect(Collectors.toList());

        return new PageResponse<>(logs, ledgerPage.getTotalElements(), null, ledgerPage.hasNext());
    }

    private InventoryDetailResponse toInventoryDetailResponse(Inventory inv) {
        ProductVariant variant = variantRepository.findById(inv.getVariantId()).orElse(null);
        Product product = variant != null ? variant.getProduct() : null;

        String sku = variant != null ? variant.getSku() : "";
        String productTitle = product != null ? product.getTitle() : "Unknown Product";

        String primaryImageUrl = null;
        if (product != null) {
            primaryImageUrl = imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId()).stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElseGet(() -> imageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId()).stream()
                    .map(ProductImage::getUrl)
                    .findFirst()
                    .orElse(null));
        }

        int avail = inv.getAvailableQty();
        int safety = inv.getSafetyBuffer() != null ? inv.getSafetyBuffer() : 10;
        String status;
        if (avail <= 0) {
            status = "OUT_OF_STOCK";
        } else if (avail <= safety) {
            status = "LOW_STOCK";
        } else {
            status = "IN_STOCK";
        }

        return InventoryDetailResponse.builder()
            .id(inv.getId())
            .variantId(inv.getVariantId())
            .sellerId(inv.getSellerId())
            .warehouseId(inv.getWarehouseId())
            .sku(sku)
            .productTitle(productTitle)
            .primaryImageUrl(primaryImageUrl)
            .physicalQty(inv.getPhysicalQty())
            .reservedQty(inv.getReservedQty())
            .damagedQty(inv.getDamagedQty())
            .availableQty(avail)
            .safetyBuffer(safety)
            .stockStatus(status)
            .updatedAt(inv.getUpdatedAt())
            .build();
    }
}

