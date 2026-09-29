-- =====================================================================
-- V1__create_schemas_and_core_tables.sql
-- Initial database migration for the eCommerce Marketplace
-- Creates all schemas and core tables per the architecture document
-- =====================================================================

-- ═══════════════════════════════════════════════════════════════
-- SCHEMAS
-- ═══════════════════════════════════════════════════════════════
CREATE SCHEMA IF NOT EXISTS identity_schema;
CREATE SCHEMA IF NOT EXISTS catalog_schema;
CREATE SCHEMA IF NOT EXISTS inventory_schema;
CREATE SCHEMA IF NOT EXISTS order_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE SCHEMA IF NOT EXISTS cart_schema;
CREATE SCHEMA IF NOT EXISTS seller_schema;
CREATE SCHEMA IF NOT EXISTS finance_schema;
CREATE SCHEMA IF NOT EXISTS audit_schema;

-- ═══════════════════════════════════════════════════════════════
-- ShedLock table (distributed scheduling)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMPTZ  NOT NULL,
    locked_at  TIMESTAMPTZ  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);

-- ═══════════════════════════════════════════════════════════════
-- IDENTITY SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE identity_schema.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(15) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE,
    name            VARCHAR(255),
    user_type       VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    password_hash   VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE identity_schema.user_roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES identity_schema.users(id),
    role            VARCHAR(50) NOT NULL,
    resource_id     UUID,
    resource_type   VARCHAR(50),
    granted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by      UUID REFERENCES identity_schema.users(id),
    UNIQUE(user_id, role, resource_id)
);
CREATE INDEX idx_user_roles_user ON identity_schema.user_roles(user_id);

CREATE TABLE identity_schema.user_addresses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES identity_schema.users(id),
    label           VARCHAR(50),
    line1           VARCHAR(255) NOT NULL,
    line2           VARCHAR(255),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    pincode         VARCHAR(10) NOT NULL,
    country         VARCHAR(3) NOT NULL DEFAULT 'IN',
    lat             DECIMAL(10,7),
    lng             DECIMAL(10,7),
    is_default      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_addresses_user ON identity_schema.user_addresses(user_id);

CREATE TABLE identity_schema.otp_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone           VARCHAR(15) NOT NULL,
    otp_hash        VARCHAR(255) NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    purpose         VARCHAR(30) NOT NULL,
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 3,
    expires_at      TIMESTAMPTZ NOT NULL,
    verified_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_otp_phone_purpose ON identity_schema.otp_requests(phone, purpose, created_at DESC);

CREATE TABLE identity_schema.refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES identity_schema.users(id),
    token_hash      VARCHAR(255) UNIQUE NOT NULL,
    token_family    UUID NOT NULL,
    device_info     VARCHAR(500),
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN DEFAULT FALSE,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON identity_schema.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON identity_schema.refresh_tokens(token_family);

-- ═══════════════════════════════════════════════════════════════
-- SELLER SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE seller_schema.sellers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID UNIQUE NOT NULL,
    business_name   VARCHAR(255) NOT NULL,
    business_type   VARCHAR(50),
    gst_number      VARCHAR(15),
    pan_number      VARCHAR(10),
    status          VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
    onboarding_status VARCHAR(30) NOT NULL DEFAULT 'INCOMPLETE',
    commission_rate_bps INT NOT NULL DEFAULT 1000,
    rating          DECIMAL(3,2) DEFAULT 0,
    total_orders    BIGINT DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE seller_schema.seller_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL REFERENCES seller_schema.sellers(id),
    document_type   VARCHAR(50) NOT NULL,
    file_url        VARCHAR(500) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verified_by     UUID,
    verified_at     TIMESTAMPTZ,
    rejection_reason TEXT,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_seller_docs_seller ON seller_schema.seller_documents(seller_id);

CREATE TABLE seller_schema.seller_bank_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL REFERENCES seller_schema.sellers(id),
    account_holder  VARCHAR(255) NOT NULL,
    account_number_encrypted VARCHAR(500) NOT NULL,
    ifsc_code       VARCHAR(11) NOT NULL,
    bank_name       VARCHAR(255) NOT NULL,
    is_verified     BOOLEAN DEFAULT FALSE,
    is_primary      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_seller_bank_seller ON seller_schema.seller_bank_accounts(seller_id);

-- ═══════════════════════════════════════════════════════════════
-- CATALOG SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE catalog_schema.categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) UNIQUE NOT NULL,
    parent_id       UUID REFERENCES catalog_schema.categories(id),
    level           INT NOT NULL DEFAULT 0,
    display_order   INT NOT NULL DEFAULT 0,
    image_url       VARCHAR(500),
    is_active       BOOLEAN DEFAULT TRUE,
    attributes_template JSONB,
    return_window_days INT DEFAULT 7,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE catalog_schema.brands (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(255) UNIQUE NOT NULL,
    logo_url        VARCHAR(500),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE catalog_schema.products (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(500) NOT NULL,
    slug            VARCHAR(500) UNIQUE NOT NULL,
    description     TEXT,
    category_id     UUID NOT NULL REFERENCES catalog_schema.categories(id),
    brand_id        UUID REFERENCES catalog_schema.brands(id),
    attributes      JSONB,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    rejection_reason TEXT,
    reviewed_by     UUID,
    reviewed_at     TIMESTAMPTZ,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_products_category ON catalog_schema.products(category_id);
CREATE INDEX idx_products_brand ON catalog_schema.products(brand_id);
CREATE INDEX idx_products_status ON catalog_schema.products(status);
CREATE INDEX idx_products_created_by ON catalog_schema.products(created_by);

CREATE TABLE catalog_schema.product_variants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES catalog_schema.products(id),
    sku             VARCHAR(100) UNIQUE NOT NULL,
    variant_attributes JSONB NOT NULL,
    weight_grams    INT,
    dimensions_cm   JSONB,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_variants_product ON catalog_schema.product_variants(product_id);

CREATE TABLE catalog_schema.product_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID NOT NULL REFERENCES catalog_schema.products(id),
    variant_id      UUID REFERENCES catalog_schema.product_variants(id),
    url             VARCHAR(500) NOT NULL,
    cloudinary_public_id VARCHAR(255),
    display_order   INT NOT NULL DEFAULT 0,
    alt_text        VARCHAR(255),
    is_primary      BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_product_images_product ON catalog_schema.product_images(product_id);

CREATE TABLE catalog_schema.seller_listings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL,
    variant_id      UUID NOT NULL REFERENCES catalog_schema.product_variants(id),
    mrp_paisa       BIGINT NOT NULL,
    selling_price_paisa BIGINT NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    condition       VARCHAR(20) DEFAULT 'NEW',
    fulfillment_type VARCHAR(20) DEFAULT 'MARKETPLACE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(seller_id, variant_id)
);
CREATE INDEX idx_seller_listings_variant ON catalog_schema.seller_listings(variant_id, is_active);
CREATE INDEX idx_seller_listings_seller ON catalog_schema.seller_listings(seller_id);

-- ═══════════════════════════════════════════════════════════════
-- INVENTORY SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE inventory_schema.inventory (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id      UUID NOT NULL,
    seller_id       UUID NOT NULL,
    warehouse_id    UUID NOT NULL,
    physical_qty    INT NOT NULL DEFAULT 0 CHECK (physical_qty >= 0),
    reserved_qty    INT NOT NULL DEFAULT 0 CHECK (reserved_qty >= 0),
    damaged_qty     INT NOT NULL DEFAULT 0 CHECK (damaged_qty >= 0),
    CONSTRAINT available_non_negative CHECK (physical_qty - reserved_qty - damaged_qty >= 0),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0,
    UNIQUE(variant_id, seller_id, warehouse_id)
);
CREATE INDEX idx_inventory_variant_seller ON inventory_schema.inventory(variant_id, seller_id);

CREATE TABLE inventory_schema.inventory_reservations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id    UUID NOT NULL REFERENCES inventory_schema.inventory(id),
    order_id        UUID,
    cart_id         UUID,
    reservation_key UUID UNIQUE NOT NULL,
    qty             INT NOT NULL CHECK (qty > 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'HELD',
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    released_at     TIMESTAMPTZ
);
CREATE INDEX idx_reservations_status_expires ON inventory_schema.inventory_reservations(status, expires_at)
    WHERE status = 'HELD';
CREATE INDEX idx_reservations_inventory ON inventory_schema.inventory_reservations(inventory_id);

CREATE TABLE inventory_schema.inventory_ledger (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id    UUID NOT NULL REFERENCES inventory_schema.inventory(id),
    event_type      VARCHAR(50) NOT NULL,
    qty_change      INT NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    UUID,
    reason          TEXT,
    actor_id        UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_inv_ledger_inventory ON inventory_schema.inventory_ledger(inventory_id, created_at);

-- ═══════════════════════════════════════════════════════════════
-- CART SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE cart_schema.carts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID UNIQUE NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE cart_schema.cart_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id         UUID NOT NULL REFERENCES cart_schema.carts(id),
    variant_id      UUID NOT NULL,
    seller_listing_id UUID NOT NULL,
    qty             INT NOT NULL DEFAULT 1 CHECK (qty > 0),
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(cart_id, seller_listing_id)
);
CREATE INDEX idx_cart_items_cart ON cart_schema.cart_items(cart_id);

-- ═══════════════════════════════════════════════════════════════
-- ORDER SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE order_schema.orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(20) UNIQUE NOT NULL,
    customer_id     UUID NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    shipping_address_snapshot JSONB NOT NULL,
    subtotal_paisa  BIGINT NOT NULL,
    discount_paisa  BIGINT NOT NULL DEFAULT 0,
    tax_paisa       BIGINT NOT NULL,
    shipping_paisa  BIGINT NOT NULL,
    total_paisa     BIGINT NOT NULL,
    coupon_code     VARCHAR(50),
    notes           TEXT,
    idempotency_key UUID UNIQUE NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_customer ON order_schema.orders(customer_id, created_at DESC);
CREATE INDEX idx_orders_status ON order_schema.orders(status);

CREATE TABLE order_schema.order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES order_schema.orders(id),
    variant_id      UUID NOT NULL,
    seller_id       UUID NOT NULL,
    seller_listing_id UUID NOT NULL,
    product_snapshot JSONB NOT NULL,
    qty             INT NOT NULL CHECK (qty > 0),
    unit_price_paisa BIGINT NOT NULL,
    mrp_paisa       BIGINT NOT NULL,
    discount_paisa  BIGINT NOT NULL DEFAULT 0,
    tax_paisa       BIGINT NOT NULL,
    total_paisa     BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    reservation_id  UUID,
    fulfillment_warehouse_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_order_items_order ON order_schema.order_items(order_id);
CREATE INDEX idx_order_items_seller ON order_schema.order_items(seller_id, status);

-- ═══════════════════════════════════════════════════════════════
-- PAYMENT SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE payment_schema.payment_intents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL,
    amount_paisa    BIGINT NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    method          VARCHAR(30),
    status          VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    razorpay_order_id   VARCHAR(100) UNIQUE,
    razorpay_payment_id VARCHAR(100),
    razorpay_signature  VARCHAR(255),
    gateway_response    JSONB,
    idempotency_key     UUID UNIQUE NOT NULL,
    failure_reason      TEXT,
    authorized_at   TIMESTAMPTZ,
    captured_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_payment_intents_order ON payment_schema.payment_intents(order_id);

CREATE TABLE payment_schema.refunds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_intent_id UUID NOT NULL REFERENCES payment_schema.payment_intents(id),
    order_id        UUID NOT NULL,
    order_item_id   UUID,
    amount_paisa    BIGINT NOT NULL,
    reason          VARCHAR(100) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    razorpay_refund_id VARCHAR(100),
    idempotency_key UUID UNIQUE NOT NULL,
    initiated_by    UUID NOT NULL,
    initiated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refunds_payment ON payment_schema.refunds(payment_intent_id);
CREATE INDEX idx_refunds_order ON payment_schema.refunds(order_id);

CREATE TABLE payment_schema.webhook_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider        VARCHAR(30) NOT NULL,
    event_id        VARCHAR(255) UNIQUE NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    processed_at    TIMESTAMPTZ,
    error_message   TEXT,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════════════════════════════
-- FINANCE SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE finance_schema.ledger_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    account_type    VARCHAR(30) NOT NULL,
    owner_type      VARCHAR(30),
    owner_id        UUID,
    balance_paisa   BIGINT NOT NULL DEFAULT 0,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    is_system       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE finance_schema.ledger_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL,
    account_id      UUID NOT NULL REFERENCES finance_schema.ledger_accounts(id),
    entry_type      VARCHAR(10) NOT NULL,
    amount_paisa    BIGINT NOT NULL CHECK (amount_paisa > 0),
    description     TEXT NOT NULL,
    reference_type  VARCHAR(50) NOT NULL,
    reference_id    UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ledger_entries_txn ON finance_schema.ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account ON finance_schema.ledger_entries(account_id, created_at);

CREATE TABLE finance_schema.seller_settlements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    gross_amount_paisa BIGINT NOT NULL,
    commission_paisa BIGINT NOT NULL,
    fees_paisa      BIGINT NOT NULL,
    tax_on_commission_paisa BIGINT NOT NULL,
    refund_deductions_paisa BIGINT NOT NULL DEFAULT 0,
    net_amount_paisa BIGINT NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payout_reference VARCHAR(255),
    approved_by     UUID,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_settlements_seller ON finance_schema.seller_settlements(seller_id);

-- ═══════════════════════════════════════════════════════════════
-- AUDIT SCHEMA
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE audit_schema.audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id        UUID NOT NULL,
    actor_role      VARCHAR(50) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(50) NOT NULL,
    target_id       UUID NOT NULL,
    before_state    JSONB,
    after_state     JSONB,
    reason          TEXT,
    ip_address      INET,
    user_agent      TEXT,
    correlation_id  UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_target ON audit_schema.audit_logs(target_type, target_id, created_at DESC);
CREATE INDEX idx_audit_actor ON audit_schema.audit_logs(actor_id, created_at DESC);

-- Prevent updates and deletes on audit_logs (immutable)
CREATE OR REPLACE FUNCTION audit_schema.prevent_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable. Updates and deletes are not allowed.';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER prevent_audit_update
    BEFORE UPDATE OR DELETE ON audit_schema.audit_logs
    FOR EACH ROW
    EXECUTE FUNCTION audit_schema.prevent_modification();

-- ═══════════════════════════════════════════════════════════════
-- OUTBOX TABLE (for transactional outbox pattern)
-- Placed in public schema as it serves all modules
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    topic           VARCHAR(200) NOT NULL,
    partition_key   VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events(status, created_at) WHERE status = 'PENDING';

-- ═══════════════════════════════════════════════════════════════
-- INBOX TABLE (for consumer idempotency)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE inbox_events (
    event_id        UUID PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════════════════════════════
-- SEED DATA: System ledger accounts
-- ═══════════════════════════════════════════════════════════════

INSERT INTO finance_schema.ledger_accounts (name, account_type, owner_type, is_system)
VALUES
    ('Platform Escrow', 'ASSET', 'PLATFORM', true),
    ('Platform Revenue - Commission', 'REVENUE', 'PLATFORM', true),
    ('Platform Revenue - Fees', 'REVENUE', 'PLATFORM', true),
    ('Platform Tax Payable', 'LIABILITY', 'PLATFORM', true),
    ('Customer Receivables', 'ASSET', 'PLATFORM', true),
    ('Bank Outflow', 'ASSET', 'PLATFORM', true);
