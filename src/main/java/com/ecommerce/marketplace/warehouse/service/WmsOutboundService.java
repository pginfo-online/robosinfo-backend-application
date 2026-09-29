package com.ecommerce.marketplace.warehouse.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.delivery.model.Shipment;
import com.ecommerce.marketplace.delivery.model.ShipmentItem;
import com.ecommerce.marketplace.delivery.model.ShipmentStatus;
import com.ecommerce.marketplace.delivery.repository.ShipmentRepository;
import com.ecommerce.marketplace.warehouse.dto.*;
import com.ecommerce.marketplace.warehouse.model.*;
import com.ecommerce.marketplace.warehouse.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
public class WmsOutboundService {

    private final WarehouseRepository warehouseRepository;
    private final PicklistRepository picklistRepository;
    private final PicklistItemRepository picklistItemRepository;
    private final PackingSlipRepository packingSlipRepository;
    private final DispatchManifestRepository manifestRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public PicklistResponse generatePicklist(GeneratePicklistRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse", request.getWarehouseId().toString()));

        List<Shipment> shipments;
        if (request.getShipmentIds() != null && !request.getShipmentIds().isEmpty()) {
            shipments = shipmentRepository.findAllById(request.getShipmentIds());
        } else {
            shipments = shipmentRepository.findAll().stream()
                .filter(s -> s.getStatus() == ShipmentStatus.CREATED || s.getStatus() == ShipmentStatus.ASSIGNED)
                .collect(Collectors.toList());
        }

        if (shipments.isEmpty()) {
            throw new BusinessRuleException("No eligible shipments found for picklist generation", "NO_ELIGIBLE_SHIPMENTS");
        }

        String picklistNum = generatePicklistNumber();

        Picklist picklist = Picklist.builder()
            .picklistNumber(picklistNum)
            .warehouse(warehouse)
            .status(PicklistStatus.GENERATED)
            .assignedStaffId(request.getAssignedStaffId())
            .build();

        picklist = picklistRepository.save(picklist);

        for (Shipment shipment : shipments) {
            for (ShipmentItem item : shipment.getItems()) {
                PicklistItem pickItem = PicklistItem.builder()
                    .picklist(picklist)
                    .shipment(shipment)
                    .orderItemId(item.getOrderItemId())
                    .variantId(item.getVariantId())
                    .qtyToPick(item.getQty())
                    .qtyPicked(0)
                    .isVerified(false)
                    .build();
                picklist.getItems().add(pickItem);
            }
        }

        picklist = picklistRepository.save(picklist);
        log.info("Generated picklist {} with {} items", picklistNum, picklist.getItems().size());
        return toPicklistResponse(picklist);
    }

    @Transactional
    public PicklistItemResponse scanItem(ScanItemRequest request) {
        PicklistItem item = picklistItemRepository.findById(request.getPicklistItemId())
            .orElseThrow(() -> new ResourceNotFoundException("Picklist Item", request.getPicklistItemId().toString()));

        item.setQtyPicked(item.getQtyPicked() + 1);
        if (item.getQtyPicked() >= item.getQtyToPick()) {
            item.setIsVerified(true);
        }

        item = picklistItemRepository.save(item);

        // Check if entire picklist is picked
        Picklist picklist = item.getPicklist();
        boolean allPicked = picklist.getItems().stream().allMatch(PicklistItem::getIsVerified);
        if (allPicked) {
            picklist.setStatus(PicklistStatus.PICKED);
            picklistRepository.save(picklist);
            log.info("Picklist {} is completely PICKED", picklist.getPicklistNumber());
        } else if (picklist.getStatus() == PicklistStatus.GENERATED) {
            picklist.setStatus(PicklistStatus.IN_PROGRESS);
            picklistRepository.save(picklist);
        }

        return toItemResponse(item);
    }

    @Transactional
    public PackingSlipResponse createPackingSlip(UUID staffId, CreatePackingSlipRequest request) {
        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Shipment", request.getShipmentId().toString()));

        String slipNumber = "PAK-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
            ThreadLocalRandom.current().nextInt(100000, 999999);

        PackingSlip slip = PackingSlip.builder()
            .slipNumber(slipNumber)
            .shipment(shipment)
            .packageWeightGrams(request.getPackageWeightGrams())
            .lengthCm(request.getLengthCm())
            .widthCm(request.getWidthCm())
            .heightCm(request.getHeightCm())
            .boxType(request.getBoxType())
            .packedBy(staffId)
            .build();

        slip = packingSlipRepository.save(slip);
        log.info("Generated packing slip {} for shipment {}", slipNumber, shipment.getAwbNumber());

        return PackingSlipResponse.builder()
            .id(slip.getId())
            .slipNumber(slip.getSlipNumber())
            .shipmentId(shipment.getId())
            .packageWeightGrams(slip.getPackageWeightGrams())
            .lengthCm(slip.getLengthCm())
            .widthCm(slip.getWidthCm())
            .heightCm(slip.getHeightCm())
            .boxType(slip.getBoxType())
            .packedBy(slip.getPackedBy())
            .packedAt(slip.getPackedAt())
            .build();
    }

    @Transactional
    public DispatchManifestResponse createManifest(CreateManifestRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse", request.getWarehouseId().toString()));

        String manifestNumber = "MAN-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
            ThreadLocalRandom.current().nextInt(100000, 999999);

        int count = request.getShipmentIds() != null ? request.getShipmentIds().size() : 0;

        DispatchManifest manifest = DispatchManifest.builder()
            .manifestNumber(manifestNumber)
            .warehouse(warehouse)
            .carrierName(request.getCarrierName())
            .vehicleNumber(request.getVehicleNumber())
            .driverName(request.getDriverName())
            .driverPhone(request.getDriverPhone())
            .status(ManifestStatus.OPEN)
            .totalShipments(count)
            .build();

        manifest = manifestRepository.save(manifest);
        log.info("Created dispatch manifest {} for carrier {}", manifestNumber, request.getCarrierName());

        return toManifestResponse(manifest);
    }

    @Transactional
    public DispatchManifestResponse dispatchManifest(UUID manifestId) {
        DispatchManifest manifest = manifestRepository.findById(manifestId)
            .orElseThrow(() -> new ResourceNotFoundException("Manifest", manifestId.toString()));

        manifest.setStatus(ManifestStatus.DISPATCHED);
        manifest.setDispatchedAt(Instant.now());
        manifest = manifestRepository.save(manifest);

        log.info("Dispatch manifest {} marked as DISPATCHED", manifest.getManifestNumber());
        return toManifestResponse(manifest);
    }

    public PicklistResponse toPicklistResponse(Picklist p) {
        List<PicklistItemResponse> items = p.getItems() != null
            ? p.getItems().stream().map(this::toItemResponse).collect(Collectors.toList())
            : new ArrayList<>();

        return PicklistResponse.builder()
            .id(p.getId())
            .picklistNumber(p.getPicklistNumber())
            .warehouseId(p.getWarehouse().getId())
            .status(p.getStatus())
            .assignedStaffId(p.getAssignedStaffId())
            .items(items)
            .createdAt(p.getCreatedAt())
            .build();
    }

    public PicklistItemResponse toItemResponse(PicklistItem i) {
        return PicklistItemResponse.builder()
            .id(i.getId())
            .shipmentId(i.getShipment().getId())
            .orderItemId(i.getOrderItemId())
            .variantId(i.getVariantId())
            .locationId(i.getLocationId())
            .qtyToPick(i.getQtyToPick())
            .qtyPicked(i.getQtyPicked())
            .isVerified(i.getIsVerified())
            .build();
    }

    public DispatchManifestResponse toManifestResponse(DispatchManifest m) {
        return DispatchManifestResponse.builder()
            .id(m.getId())
            .manifestNumber(m.getManifestNumber())
            .warehouseId(m.getWarehouse().getId())
            .carrierName(m.getCarrierName())
            .vehicleNumber(m.getVehicleNumber())
            .driverName(m.getDriverName())
            .driverPhone(m.getDriverPhone())
            .status(m.getStatus())
            .totalShipments(m.getTotalShipments())
            .dispatchedAt(m.getDispatchedAt())
            .createdAt(m.getCreatedAt())
            .build();
    }

    private String generatePicklistNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "PKL-" + dateStr + "-" + rand;
    }
}
