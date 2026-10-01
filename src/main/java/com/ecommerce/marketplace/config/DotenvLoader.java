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

        normalizeDatabaseUrl();
        normalizePort();
    }

    /**
     * Converts standard PostgreSQL URLs (e.g. from Render or Supabase) into JDBC format:
     * postgres://user:pass@host:port/db -> jdbc:postgresql://host:port/db?sslmode=require
     */
    private static void normalizeDatabaseUrl() {
        String dbUrl = System.getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getProperty("DATABASE_URL");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("DB_URL");
            if (dbUrl == null || dbUrl.isBlank()) {
                dbUrl = System.getProperty("DB_URL");
            }
        }

        if (dbUrl != null && !dbUrl.isBlank()) {
            dbUrl = dbUrl.trim();
            if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
                try {
                    String httpFormat = dbUrl.replaceFirst("^postgres(ql)?://", "http://");
                    java.net.URI uri = new java.net.URI(httpFormat);

                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && !userInfo.isBlank()) {
                        String[] parts = userInfo.split(":", 2);
                        if (System.getProperty("DB_USERNAME") == null && System.getenv("DB_USERNAME") == null) {
                            System.setProperty("DB_USERNAME", parts[0]);
                        }
                        if (parts.length > 1 && System.getProperty("DB_PASSWORD") == null && System.getenv("DB_PASSWORD") == null) {
                            System.setProperty("DB_PASSWORD", parts[1]);
                        }
                    }

                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath() != null && !uri.getPath().isBlank() ? uri.getPath() : "/marketplace";
                    String query = uri.getQuery();

                    StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                            .append(host)
                            .append(":").append(port)
                            .append(path);

                    if (query != null && !query.isBlank()) {
                        jdbcUrl.append("?").append(query);
                        if (!query.contains("sslmode=")) {
                            jdbcUrl.append("&sslmode=require");
                        }
                    } else if (!"localhost".equals(host) && !"127.0.0.1".equals(host)) {
                        jdbcUrl.append("?sslmode=require");
                    }

                    String finalJdbcUrl = jdbcUrl.toString();
                    System.setProperty("DB_URL", finalJdbcUrl);
                    System.setProperty("spring.datasource.url", finalJdbcUrl);
                    System.out.println("[DotenvLoader] Normalized PostgreSQL URL to JDBC: " +
                            finalJdbcUrl.replaceAll(":[^/@]+@", ":****@"));
                } catch (Exception e) {
                    System.err.println("[DotenvLoader] Warning: Could not parse database URL: " + e.getMessage());
                }
            } else if (dbUrl.startsWith("jdbc:postgresql://")) {
                System.setProperty("DB_URL", dbUrl);
                System.setProperty("spring.datasource.url", dbUrl);
            }
        }
    }

    private static void normalizePort() {
        String port = System.getenv("PORT");
        if (port != null && !port.isBlank()) {
            if (System.getProperty("server.port") == null) {
                System.setProperty("server.port", port.trim());
            }
        }
    }
}
