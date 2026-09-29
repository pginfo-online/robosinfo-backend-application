package com.ecommerce.marketplace.identity.repository;

import com.ecommerce.marketplace.identity.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);

    void deleteByUserIdAndRole(UUID userId, String role);
}
