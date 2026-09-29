-- =====================================================================
-- V4__add_customer_wishlist.sql
-- Creates customer wishlist table in identity_schema
-- =====================================================================

CREATE TABLE IF NOT EXISTS identity_schema.customer_wishlist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES identity_schema.users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES catalog_schema.products(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_product_wishlist UNIQUE (customer_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_customer ON identity_schema.customer_wishlist(customer_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_product ON identity_schema.customer_wishlist(product_id);
