package com.ecommerce.marketplace.warehouse.service;

import com.ecommerce.marketplace.common.dto.PageResponse;
import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.inventory.dto.AdjustStockRequest;
import com.ecommerce.marketplace.inventory.model.InventoryEventType;
import com.ecommerce.marketplace.inventory.service.InventoryService;
import com.ecommerce.marketplace.warehouse.dto.*;
import com.ecommerce.marketplace.warehouse.model.*;
import com.ecommerce.marketplace.warehouse.repository.GoodsReceiptItemRepository;
import com.ecommerce.marketplace.warehouse.repository.GoodsReceiptNoteRepository;
import com.ecommerce.marketplace.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsReceiptItemRepository grnItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryService inventoryService;

    @Transactional
    public GrnResponse createGrn(UUID staffId, CreateGrnRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse", request.getWarehouseId().toString()));

        String grnNumber = generateGrnNumber();

        GoodsReceiptNote grn = GoodsReceiptNote.builder()
            .grnNumber(grnNumber)
            .warehouse(warehouse)
            .sellerId(request.getSellerId())
            .status(GrnStatus.RECEIVED)
            .consignmentReference(request.getConsignmentReference())
            .receivedBy(staffId)
            .notes(request.getNotes())
            .build();

        grn = grnRepository.save(grn);

        if (request.getItems() != null) {
            for (GrnItemRequest itemReq : request.getItems()) {
                GoodsReceiptItem item = GoodsReceiptItem.builder()
                    .grn(grn)
                    .variantId(itemReq.getVariantId())
                    .expectedQty(itemReq.getExpectedQty() != null ? itemReq.getExpectedQty() : 0)
                    .receivedQty(itemReq.getReceivedQty() != null ? itemReq.getReceivedQty() : 0)
                    .passedQty(itemReq.getPassedQty() != null ? itemReq.getPassedQty() : 0)
                    .failedQty(itemReq.getFailedQty() != null ? itemReq.getFailedQty() : 0)
                    .damagedQty(itemReq.getDamagedQty() != null ? itemReq.getDamagedQty() : 0)
                    .disposition(itemReq.getDisposition() != null ? itemReq.getDisposition() : ItemDisposition.ACCEPTED)
                    .locationId(itemReq.getLocationId())
                    .notes(itemReq.getNotes())
                    .build();
                grn.getItems().add(item);
            }
            grn = grnRepository.save(grn);
        }

        log.info("Created Inbound GRN {} for seller {} in warehouse {}", grnNumber, request.getSellerId(), warehouse.getName());
        return toResponse(grn);
    }

    @Transactional
    public GrnResponse completeGrn(UUID grnId) {
        GoodsReceiptNote grn = grnRepository.findById(grnId)
            .orElseThrow(() -> new ResourceNotFoundException("GRN", grnId.toString()));

        if (grn.getStatus() == GrnStatus.COMPLETED) {
            throw new BusinessRuleException("GRN is already completed", "GRN_ALREADY_COMPLETED");
        }

        // For all QC-passed inventory items, inward them into system inventory
        for (GoodsReceiptItem item : grn.getItems()) {
            if (item.getPassedQty() > 0 && item.getDisposition() == ItemDisposition.ACCEPTED) {
                AdjustStockRequest adjustReq = AdjustStockRequest.builder()
                    .variantId(item.getVariantId())
                    .warehouseId(grn.getWarehouse().getId())
                    .qtyChange(item.getPassedQty())
                    .eventType(InventoryEventType.STOCK_IN)
                    .reason("Inbound GRN receipt " + grn.getGrnNumber())
                    .build();

                inventoryService.adjustStock(grn.getSellerId(), adjustReq);
                log.info("Inwarded {} units of variant {} from GRN {}", item.getPassedQty(), item.getVariantId(), grn.getGrnNumber());
            }
        }

        grn.setStatus(GrnStatus.COMPLETED);
        grn = grnRepository.save(grn);
        return toResponse(grn);
    }

    @Transactional(readOnly = true)
    public GrnResponse getGrnById(UUID grnId) {
        return toResponse(grnRepository.findById(grnId)
            .orElseThrow(() -> new ResourceNotFoundException("GRN", grnId.toString())));
    }

    @Transactional(readOnly = true)
    public PageResponse<GrnResponse> getWarehouseGrns(UUID warehouseId, GrnStatus status, Pageable pageable) {
        Page<GoodsReceiptNote> page = status != null
            ? grnRepository.findByWarehouseIdAndStatus(warehouseId, status, pageable)
            : grnRepository.findByWarehouseId(warehouseId, pageable);

        List<GrnResponse> data = page.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());

        return new PageResponse<>(data, page.getTotalElements(), null, page.hasNext());
    }

    public GrnResponse toResponse(GoodsReceiptNote grn) {
        List<GrnItemResponse> items = grn.getItems() != null
            ? grn.getItems().stream().map(i -> GrnItemResponse.builder()
                .id(i.getId())
                .variantId(i.getVariantId())
                .expectedQty(i.getExpectedQty())
                .receivedQty(i.getReceivedQty())
                .passedQty(i.getPassedQty())
                .failedQty(i.getFailedQty())
                .damagedQty(i.getDamagedQty())
                .disposition(i.getDisposition())
                .locationId(i.getLocationId())
                .notes(i.getNotes())
                .build()).collect(Collectors.toList())
            : new ArrayList<>();

        return GrnResponse.builder()
            .id(grn.getId())
            .grnNumber(grn.getGrnNumber())
            .warehouseId(grn.getWarehouse().getId())
            .sellerId(grn.getSellerId())
            .status(grn.getStatus())
            .consignmentReference(grn.getConsignmentReference())
            .receivedBy(grn.getReceivedBy())
            .receivedAt(grn.getReceivedAt())
            .notes(grn.getNotes())
            .items(items)
            .createdAt(grn.getCreatedAt())
            .build();
    }

    private String generateGrnNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "GRN-" + dateStr + "-" + rand;
    }
}
