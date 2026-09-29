package com.ecommerce.marketplace.delivery.service;

import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.delivery.dto.DeliveryTrackingResponse;
import com.ecommerce.marketplace.delivery.dto.UpdateLocationRequest;
import com.ecommerce.marketplace.delivery.model.DeliveryPartnerLocation;
import com.ecommerce.marketplace.delivery.model.Shipment;
import com.ecommerce.marketplace.delivery.repository.DeliveryPartnerLocationRepository;
import com.ecommerce.marketplace.delivery.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingService {

    private final DeliveryPartnerLocationRepository locationRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public void recordLocation(UUID partnerId, UpdateLocationRequest request) {
        DeliveryPartnerLocation location = DeliveryPartnerLocation.builder()
            .deliveryPartnerId(partnerId)
            .shipmentId(request.getShipmentId())
            .lat(request.getLat())
            .lng(request.getLng())
            .speed(request.getSpeed())
            .heading(request.getHeading())
            .batteryLevel(request.getBatteryLevel())
            .build();

        locationRepository.save(location);
        log.debug("Recorded GPS location for partner {}: ({}, {})", partnerId, request.getLat(), request.getLng());
    }

    @Transactional(readOnly = true)
    public DeliveryTrackingResponse getTracking(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId.toString()));

        Optional<DeliveryPartnerLocation> locOpt = locationRepository.findFirstByShipmentIdOrderByRecordedAtDesc(shipmentId);

        if (locOpt.isEmpty() && shipment.getDeliveryPartnerId() != null) {
            locOpt = locationRepository.findFirstByDeliveryPartnerIdOrderByRecordedAtDesc(shipment.getDeliveryPartnerId());
        }

        return DeliveryTrackingResponse.builder()
            .shipmentId(shipment.getId())
            .awbNumber(shipment.getAwbNumber())
            .status(shipment.getStatus())
            .carrierName(shipment.getCarrierName())
            .currentLat(locOpt.map(DeliveryPartnerLocation::getLat).orElse(null))
            .currentLng(locOpt.map(DeliveryPartnerLocation::getLng).orElse(null))
            .lastLocationUpdate(locOpt.map(DeliveryPartnerLocation::getRecordedAt).orElse(null))
            .estimatedDeliveryAt(shipment.getEstimatedDeliveryAt())
            .deliveredAt(shipment.getDeliveredAt())
            .build();
    }
}
