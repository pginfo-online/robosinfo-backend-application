-- =====================================================================
-- V3__seller_enhancements.sql
-- Adds store settings fields to seller_schema.sellers and safety buffer to inventory
-- =====================================================================

ALTER TABLE seller_schema.sellers ADD COLUMN IF NOT EXISTS support_email VARCHAR(255);
ALTER TABLE seller_schema.sellers ADD COLUMN IF NOT EXISTS support_phone VARCHAR(50);
ALTER TABLE seller_schema.sellers ADD COLUMN IF NOT EXISTS store_description VARCHAR(1000);
ALTER TABLE seller_schema.sellers ADD COLUMN IF NOT EXISTS pickup_address VARCHAR(500);
ALTER TABLE seller_schema.sellers ADD COLUMN IF NOT EXISTS return_policy_days INT DEFAULT 7;

ALTER TABLE inventory_schema.inventory ADD COLUMN IF NOT EXISTS safety_buffer INT DEFAULT 10;
