-- V5: Add description and commission_rate_percent to categories table
-- These fields were missing from V1 but are required by the admin frontend

ALTER TABLE catalog_schema.categories
    ADD COLUMN IF NOT EXISTS description         TEXT,
    ADD COLUMN IF NOT EXISTS commission_rate_percent NUMERIC(5, 2) DEFAULT 10.00;

COMMENT ON COLUMN catalog_schema.categories.description IS 'Optional human-readable description shown in storefront navigation';
COMMENT ON COLUMN catalog_schema.categories.commission_rate_percent IS 'Platform commission charged on sales in this category (e.g. 10.00 = 10%)';
