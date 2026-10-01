package com.ecommerce.marketplace.identity.initializer;

import com.ecommerce.marketplace.identity.model.RoleName;
import com.ecommerce.marketplace.identity.model.User;
import com.ecommerce.marketplace.identity.model.UserStatus;
import com.ecommerce.marketplace.identity.model.UserType;
import com.ecommerce.marketplace.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.seed.email:admin@shopmart.in}")
    private String adminEmail;

    @Value("${app.admin.seed.password:Ms24GyFCadRfUS4FcJ1XESXgScQA19zC}")
    private String adminPassword;

    @Value("${app.admin.seed.phone:0000000000}")
    private String adminPhone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
            admin -> {
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setUserType(UserType.ADMIN);
                admin.setStatus(UserStatus.ACTIVE);
                admin.addRole(RoleName.ROLE_ADMIN);
                admin.addRole(RoleName.ROLE_SUPER_ADMIN);
                userRepository.save(admin);
                log.info("Super Admin account verified and synchronized with ROLE_ADMIN & ROLE_SUPER_ADMIN for: {}", adminEmail);
            },
            () -> {
                if (!userRepository.existsByPhone(adminPhone)) {
                    User admin = User.builder()
                        .name("Super Administrator")
                        .email(adminEmail)
                        .phone(adminPhone)
                        .userType(UserType.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .passwordHash(passwordEncoder.encode(adminPassword))
                        .build();
                    admin.addRole(RoleName.ROLE_ADMIN);
                    admin.addRole(RoleName.ROLE_SUPER_ADMIN);
                    userRepository.save(admin);
                    log.info("Initialized default Super Admin account with email: {}", adminEmail);
                } else {
                    log.warn("Cannot initialize Super Admin: phone {} already registered under different email", adminPhone);
                }
            }
        );
    }
}
