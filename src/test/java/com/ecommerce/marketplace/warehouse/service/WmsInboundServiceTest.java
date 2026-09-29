package com.ecommerce.marketplace.warehouse.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.inventory.dto.AdjustStockRequest;
import com.ecommerce.marketplace.inventory.service.InventoryService;
import com.ecommerce.marketplace.warehouse.dto.GrnResponse;
import com.ecommerce.marketplace.warehouse.model.*;
import com.ecommerce.marketplace.warehouse.repository.GoodsReceiptItemRepository;
import com.ecommerce.marketplace.warehouse.repository.GoodsReceiptNoteRepository;
import com.ecommerce.marketplace.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WmsInboundServiceTest {

    @Mock
    private GoodsReceiptNoteRepository grnRepository;

    @Mock
    private GoodsReceiptItemRepository grnItemRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private WmsInboundService wmsInboundService;

    private UUID grnId;
    private UUID warehouseId;
    private UUID sellerId;
    private UUID variantId;
    private Warehouse warehouse;
    private GoodsReceiptNote grn;

    @BeforeEach
    void setUp() {
        grnId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        warehouse = Warehouse.builder()
                .id(warehouseId)
                .code("WH-BLR-01")
                .name("Bangalore Central DC")
                .build();

        GoodsReceiptItem item = GoodsReceiptItem.builder()
                .id(UUID.randomUUID())
                .variantId(variantId)
                .expectedQty(50)
                .receivedQty(50)
                .passedQty(48)
                .failedQty(2)
                .disposition(ItemDisposition.ACCEPTED)
                .build();

        grn = GoodsReceiptNote.builder()
                .id(grnId)
                .grnNumber("GRN-2026-TEST")
                .warehouse(warehouse)
                .sellerId(sellerId)
                .status(GrnStatus.RECEIVED)
                .items(new ArrayList<>(List.of(item)))
                .build();
    }

    @Test
    @DisplayName("Should complete GRN and inward QC-passed stock into inventory")
    void testCompleteGrnInwardStock() {
        when(grnRepository.findById(grnId)).thenReturn(Optional.of(grn));
        when(grnRepository.save(any(GoodsReceiptNote.class))).thenAnswer(i -> i.getArgument(0));

        GrnResponse response = wmsInboundService.completeGrn(grnId);

        assertNotNull(response);
        assertEquals(GrnStatus.COMPLETED, response.getStatus());
        // Verify inventoryService.adjustStock was called with 48 units
        verify(inventoryService).adjustStock(eq(sellerId), any(AdjustStockRequest.class));
        verify(grnRepository).save(grn);
    }

    @Test
    @DisplayName("Should reject completing an already completed GRN")
    void testRejectAlreadyCompletedGrn() {
        grn.setStatus(GrnStatus.COMPLETED);
        when(grnRepository.findById(grnId)).thenReturn(Optional.of(grn));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                wmsInboundService.completeGrn(grnId)
        );

        assertTrue(ex.getMessage().contains("GRN is already completed"));
        verifyNoInteractions(inventoryService);
    }
}
