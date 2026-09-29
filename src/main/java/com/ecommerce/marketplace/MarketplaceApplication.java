package com.ecommerce.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import com.ecommerce.marketplace.config.DotenvLoader;

@SpringBootApplication
@EnableAsync
public class MarketplaceApplication {

    public static void main(String[] args) {
        DotenvLoader.load();
        SpringApplication.run(MarketplaceApplication.class, args);
    }
}
