package com.ecommerce.marketplace.identity.service;

import com.ecommerce.marketplace.common.exception.ResourceNotFoundException;
import com.ecommerce.marketplace.identity.dto.AddressRequest;
import com.ecommerce.marketplace.identity.dto.AddressResponse;
import com.ecommerce.marketplace.identity.dto.UserProfileResponse;
import com.ecommerce.marketplace.identity.model.User;
import com.ecommerce.marketplace.identity.model.UserAddress;
import com.ecommerce.marketplace.identity.model.UserRole;
import com.ecommerce.marketplace.identity.repository.UserAddressRepository;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, String name, String email) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }

        user = userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        user.setStatus(com.ecommerce.marketplace.identity.model.UserStatus.DELETED);
        user.setName("Deleted User");
        user.setEmail(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(UUID userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::toAddressResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.resetDefaultAddresses(userId);
        }

        UserAddress address = UserAddress.builder()
            .userId(userId)
            .label(request.getLabel())
            .line1(request.getLine1())
            .line2(request.getLine2())
            .city(request.getCity())
            .state(request.getState())
            .pincode(request.getPincode())
            .country(request.getCountry() != null ? request.getCountry() : "IN")
            .lat(request.getLat())
            .lng(request.getLng())
            .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
            .build();

        address = addressRepository.save(address);
        return toAddressResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ADDRESS_NOT_FOUND", "Address not found"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.resetDefaultAddresses(userId);
            address.setIsDefault(true);
        }

        address.setLabel(request.getLabel());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        address.setLat(request.getLat());
        address.setLng(request.getLng());

        address = addressRepository.save(address);
        return toAddressResponse(address);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
        addressRepository.delete(address);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .phone(user.getPhone())
            .email(user.getEmail())
            .name(user.getName())
            .userType(user.getUserType())
            .status(user.getStatus())
            .roles(user.getRoles().stream().map(UserRole::getRole).collect(Collectors.toSet()))
            .build();
    }

    private AddressResponse toAddressResponse(UserAddress address) {
        return AddressResponse.builder()
            .id(address.getId())
            .label(address.getLabel())
            .line1(address.getLine1())
            .line2(address.getLine2())
            .city(address.getCity())
            .state(address.getState())
            .pincode(address.getPincode())
            .country(address.getCountry())
            .lat(address.getLat())
            .lng(address.getLng())
            .isDefault(address.getIsDefault())
            .createdAt(address.getCreatedAt())
            .build();
    }
}
