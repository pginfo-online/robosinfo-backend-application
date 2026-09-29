package com.ecommerce.marketplace.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Production-safe lightweight .env loader for Spring Boot.
 * Reads environment variables from a local .env file and injects them
 * into Java System Properties ONLY if they are not already set in OS environment
 * variables or System properties.
 */
public final class DotenvLoader {

    private DotenvLoader() {
        // Utility class
    }

    public static void load() {
        List<File> candidates = new ArrayList<>();

        // Check custom environment property
        String customPath = System.getProperty("ENV_FILE");
        if (customPath != null && !customPath.isBlank()) {
            candidates.add(new File(customPath));
        }

        // Standard locations
        candidates.add(new File(".env"));
        candidates.add(new File("backend/.env"));
        candidates.add(new File("../.env"));

        File envFile = null;
        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                envFile = candidate;
                break;
            }
        }

        if (envFile == null) {
            return;
        }

        int loadedCount = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(envFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }

                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                // Strip outer double or single quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) ||
                    (value.startsWith("'") && value.endsWith("'") && value.length() >= 2)) {
                    value = value.substring(1, value.length() - 1);
                }

                // Only set if not already defined in OS env or System properties
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    loadedCount++;
                }
            }
            System.out.printf("[DotenvLoader] Successfully loaded %d properties from: %s%n",
                    loadedCount, envFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.printf("[DotenvLoader] Warning: Failed to load .env file from %s: %s%n",
                    envFile.getAbsolutePath(), e.getMessage());
        }
    }
}
