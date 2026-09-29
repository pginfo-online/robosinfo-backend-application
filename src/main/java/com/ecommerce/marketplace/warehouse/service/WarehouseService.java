package com.ecommerce.marketplace.warehouse.service;

import com.ecommerce.marketplace.common.exception.BusinessRuleException;
import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.warehouse.dto.CreateWarehouseRequest;
import com.ecommerce.marketplace.warehouse.dto.WarehouseResponse;
import com.ecommerce.marketplace.warehouse.model.Warehouse;
import com.ecommerce.marketplace.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        if (warehouseRepository.findByCode(request.getCode()).isPresent()) {
            throw new BusinessRuleException("Warehouse code already exists", "DUPLICATE_WAREHOUSE_CODE");
        }

        Warehouse warehouse = Warehouse.builder()
            .name(request.getName())
            .code(request.getCode())
            .addressLine1(request.getAddressLine1())
            .addressLine2(request.getAddressLine2())
            .city(request.getCity())
            .state(request.getState())
            .pincode(request.getPincode())
            .contactNumber(request.getContactNumber())
            .email(request.getEmail())
            .isActive(true)
            .build();

        warehouse = warehouseRepository.save(warehouse);
        log.info("Created warehouse {} with code {}", warehouse.getName(), warehouse.getCode());
        return toResponse(warehouse);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Warehouse", id.toString()));
        return toResponse(warehouse);
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllActiveWarehouses() {
        return warehouseRepository.findByIsActiveTrue().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public WarehouseResponse toResponse(Warehouse w) {
        return WarehouseResponse.builder()
            .id(w.getId())
            .name(w.getName())
            .code(w.getCode())
            .addressLine1(w.getAddressLine1())
            .addressLine2(w.getAddressLine2())
            .city(w.getCity())
            .state(w.getState())
            .pincode(w.getPincode())
            .contactNumber(w.getContactNumber())
            .email(w.getEmail())
            .isActive(w.getIsActive())
            .createdAt(w.getCreatedAt())
            .build();
    }
}
