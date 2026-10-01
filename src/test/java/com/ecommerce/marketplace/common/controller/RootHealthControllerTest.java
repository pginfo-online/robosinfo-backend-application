package com.ecommerce.marketplace.common.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RootHealthControllerTest {

    private final RootHealthController controller = new RootHealthController();

    @Test
    @DisplayName("Root endpoint should return HTTP 200 with UP status")
    void testRootEndpoint() {
        ResponseEntity<Map<String, Object>> response = controller.root();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("marketplace-backend", response.getBody().get("service"));
    }

    @Test
    @DisplayName("Health endpoint should return HTTP 200 with UP status")
    void testHealthEndpoint() {
        ResponseEntity<Map<String, Object>> response = controller.health();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }
}
