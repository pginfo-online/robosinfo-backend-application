package com.ecommerce.marketplace.config;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    @Value("${app.shedlock.enabled:false}")
    private boolean shedLockEnabled;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        if (!shedLockEnabled) {
            return new LockProvider() {
                @Override
                public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
                    return Optional.of(new SimpleLock() {
                        @Override
                        public void unlock() {
                            // No-op for single instance / dev mode
                        }
                    });
                }
            };
        }
        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
                .withTableName("public.shedlock")
                .usingDbTime()
                .build()
        );
    }
}
